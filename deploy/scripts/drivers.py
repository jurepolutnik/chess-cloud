from abc import ABCMeta, abstractmethod
from libcloud.compute.types import Provider
from ConfigParser import SafeConfigParser

conf = SafeConfigParser()
conf.read('conf.ini')
conf.read('conf_local.ini')
conf.read('conf_local2.ini')

class LibCloudWrapper(object):
	__metaclass__ = ABCMeta

	def __init__ (self, driver_key):
		self.driver_key = driver_key

	@abstractmethod
	def initialize (self):
		pass

	@abstractmethod
	def get_driver (self):
		pass

	def get_property (self, key):
		return conf.get(self.driver_key, key)


class AwsWrapper (LibCloudWrapper):
	def __init__ (self):
		super(AwsWrapper, self).__init__('AWS')

	def initialize(self):
		print 'Initializing AWS driver.'

		from libcloud.compute.providers import get_driver
		AWS = get_driver(Provider.EC2_EU_WEST)

		self.driver = AWS (self.get_property('ACCESS_ID'), self.get_property('SECRET_ID'))

	def get_driver(self):
		return self.driver


class OpenStackWrapper (LibCloudWrapper):
	def __init__ (self):
		super(OpenStackWrapper, self).__init__('OpenStack')

	def initialize(self):
		print 'Initializing OpenStack driver.'

		from libcloud.compute.providers import get_driver
		OpenStack = get_driver(Provider.OPENSTACK)
		self.driver = OpenStack (
			self.get_property('username'),
			self.get_property ('password'),
			ex_tenant_name=self.get_property ('ex_tenant_name'),
			ex_force_auth_url=self.get_property ('ex_force_auth_url'),
			ex_force_auth_version='2.0_password',
			ex_force_service_name='newnova',
		)

	def get_driver(self):
		return self.driver


def get_wrapper ():
	driver = conf.get('DEFAULT', 'DRIVER')
	if driver == 'AWS':
		return AwsWrapper()

	if driver == 'OpenStack':
		return OpenStackWrapper()
