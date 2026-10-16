package au.org.ala.collectory

import grails.testing.mixin.integration.Integration
import groovy.sql.Sql
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.Database
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import spock.lang.PendingFeature
import spock.lang.Requires
import spock.lang.Specification

import javax.sql.DataSource

/**
 * The app boots with grails.plugin.databasemigration.updateOnStart, so by the time a
 * feature runs Liquibase has already applied grails-app/migrations/changelog.xml to the
 * MySQL given by COLLECTORY_TEST_DB_URL. Run it against an empty database
 * (src/test/docker/mysql-compose.yml keeps data in tmpfs) to cover a clean install.
 */
@Integration
@Requires({ System.getenv('COLLECTORY_TEST_DB_URL') })
class LiquibaseMigrationIntegrationSpec extends Specification {

    // Same path the plugin uses (updateOnStartFileName); it is part of each changeset's identity
    static final String CHANGELOG = 'changelog.xml'

    DataSource dataSource

    void "the app runs against MySQL, not the H2 fallback"() {
        expect:
        dataSource.connection.withCloseable { it.metaData.databaseProductName } == 'MySQL'
    }

    void "a clean install applies every changeset of the changelog"() {
        when:
        List<Map> rows = withSql { Sql sql ->
            sql.rows('select ID, AUTHOR, EXECTYPE from DATABASECHANGELOG')
        }
        int changeSetsInChangelog = withLiquibase { Liquibase lb -> lb.databaseChangeLog.changeSets.size() }

        then:
        rows.size() == changeSetsInChangelog
        // MARK_RAN: preconditions find a column/index that initial.sql already creates
        rows*.EXECTYPE.toSet() - ['EXECUTED', 'MARK_RAN'] == [] as Set

        and: 'initial.sql and the 5.1.0*.sql files were loaded'
        tableNames().containsAll(['data_resource', 'collection', 'institution', 'contact',
                                  'collection_external_identifier'])
    }

    void "a second run applies no changeset and keeps checksums valid"() {
        given:
        int before = changelogRowCount()

        expect:
        withLiquibase { Liquibase lb -> lb.listUnrunChangeSets(new Contexts(), new LabelExpression()) } == []

        when:
        withLiquibase { Liquibase lb ->
            lb.validate()
            lb.update(new Contexts(), new LabelExpression())
        }

        then:
        changelogRowCount() == before
    }

    @PendingFeature(reason = 'Tables are created as utf8mb3 (CHARSET=utf8 in initial.sql and 5.1.0*.sql): characters outside the BMP cannot be stored, e.g. CJK Extension B ideographs used in some Chinese and Japanese names (𠮷, 𩸽). Moving to utf8mb4 is an ops decision.')
    void "every table uses utf8mb4"() {
        when:
        Map<String, String> collations = withSql { Sql sql ->
            sql.rows('''select TABLE_NAME, TABLE_COLLATION from information_schema.TABLES
                        where TABLE_SCHEMA = database()''')
                    .collectEntries { [(it.TABLE_NAME): it.TABLE_COLLATION] }
        }
        println "Effective table collations: ${collations.groupBy { it.value }.collectEntries { k, v -> [(k): v.size()] }}"

        then:
        collations.findAll { !it.value.startsWith('utf8mb4') } == [:]
    }

    private <T> T withSql(Closure<T> work) {
        Sql sql = new Sql(dataSource)
        try {
            work(sql)
        } finally {
            sql.close()
        }
    }

    private <T> T withLiquibase(Closure<T> work) {
        Database database = DatabaseFactory.instance
                .findCorrectDatabaseImplementation(new JdbcConnection(dataSource.connection))
        try {
            work(new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database))
        } finally {
            database.close()
        }
    }

    private int changelogRowCount() {
        withSql { Sql sql -> sql.firstRow('select count(*) n from DATABASECHANGELOG').n as int }
    }

    private Set<String> tableNames() {
        withSql { Sql sql ->
            sql.rows('select TABLE_NAME from information_schema.TABLES where TABLE_SCHEMA = database()')
                    *.TABLE_NAME.collect { it.toLowerCase() }.toSet()
        }
    }
}
