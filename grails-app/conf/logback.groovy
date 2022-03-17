import ch.qos.logback.core.util.FileSize
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import java.nio.charset.StandardCharsets

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter

logDir = "/tmp/"

if (new File("/var/log/tomcat9/").exists()){
    logDir = "/var/log/tomcat9/"
}

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = StandardCharsets.UTF_8
        pattern =
                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
                '%clr(%5p) ' + // Log level
                '%clr(---){faint} %clr([%15.15t]){faint} ' + // Thread
                '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                '%m%n%wex' // Message
    }
}

appender('COLLECTORY_LOG', RollingFileAppender) {
    file = logDir + "collectory.log"
    encoder(PatternLayoutEncoder) {
        pattern =
                '%d{yyyy-MM-dd HH:mm:ss.SSS} ' + // Date
                        '%5p ' + // Log level
                        '--- [%15.15t] ' + // Thread
                        '%-40.40logger{39} : ' + // Logger
                        '%m%n%wex' // Message
    }
    rollingPolicy(FixedWindowRollingPolicy) {
        fileNamePattern = logDir + "collectory.log.%i.log.gz"
        minIndex=1
        maxIndex=4
    }
    triggeringPolicy(SizeBasedTriggeringPolicy) {
        maxFileSize = FileSize.valueOf('10MB')
    }
}

root(INFO, ['STDOUT'])


final error = [
        'org.springframework',
        'grails.app',
        'grails.plugins.mail',
        'org.hibernate',
        'org.quartz',
        'asset.pipeline'
]
final warn = [
        'au.org.ala.cas'

]
final info = [
        'au.org.ala.collectory',
        'org.liquibase',
        'liquibase'
]

final debug = []
final trace = []

for (def name : error) logger(name, ERROR, ['COLLECTORY_LOG', 'STDOUT'], false)
for (def name : warn) logger(name, WARN, ['COLLECTORY_LOG','STDOUT'], false)
for (def name: info) logger(name, INFO,['COLLECTORY_LOG','STDOUT'], false)
for (def name: debug) logger(name, DEBUG,['COLLECTORY_LOG','STDOUT'], false)
for (def name: trace) logger(name, TRACE,['COLLECTORY_LOG','STDOUT'], false)

