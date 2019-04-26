def call(body) {
	
	final MAIL_DEFAULT_RECIPIENT = new String('c3RlcGhhbi5zZWlmZXJtYW5uQGtpdC5lZHU='.decodeBase64())
	final BUILD_IMAGE = 'maven:3-jdk-11'
	final BUILD_LIMIT_TIME = 30
	final BUILD_LIMIT_RAM = '4G'
	final BUILD_LIMIT_HDD = '20G'
	final SSH_NAME = 'web'
	final WEB_ROOT = '/home/deploy/html'
	
	slaveEclipsePipeline(body, SSH_NAME, WEB_ROOT, MAIL_DEFAULT_RECIPIENT, BUILD_IMAGE, BUILD_LIMIT_TIME, BUILD_LIMIT_RAM, BUILD_LIMIT_HDD)

}