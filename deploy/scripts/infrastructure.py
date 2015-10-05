##################################################################################################
# Libcloud - creating/destroying/ ...
#
from libcloud.compute.types import Provider
from libcloud.compute.providers import get_driver
from libcloud.compute.base import NodeAuthPassword
from libcloud.compute.deployment import SSHKeyDeployment
from libcloud.compute.deployment import MultiStepDeployment
from libcloud.compute.deployment import ScriptDeployment, SSHKeyDeployment
from libcloud.compute.types import NodeState

from fabric.api import *

import os


CLUSTER_NAME_PREFIX = 'chesscloud-'
WORKER_NAME_PREFIX  =  CLUSTER_NAME_PREFIX + 'worker-'
MASTER_NAME_PREFIX  =  CLUSTER_NAME_PREFIX + 'master-'
FRONTEND_NAME_PREFIX  =  CLUSTER_NAME_PREFIX + 'frontend-'

nodes = None
images = None
sizes = None
driver = None

###########################################################################
#
# Nodes manage
#

@task
def ssh ():

	select()

	print "Opening shell at " + env.host_string + " through " + env.gateway

	open_shell("/bin/bash")

@task
def cmd(cmd):

	select()

	print "Running command on " + env.host_string

	run (cmd)


###########################################################################
#
# Nodes selection
#

@task
def all():
	env_hosts('')

@task
def cluster():
	env_hosts(CLUSTER_NAME_PREFIX)

@task
def masters():
	env.role='master'
	env_hosts(MASTER_NAME_PREFIX)

@task
def workers():
	env.role='worker'
	env_hosts(WORKER_NAME_PREFIX)

@task
def frontends():
	env.role='frontend'
	env_hosts(FRONTEND_NAME_PREFIX)

def env_hosts (prefix):
	cluster_nodes = get_nodes(prefix='')
	env.seeds=[]
	env.hosts=[]
	env.host_names={}
	env.host_roles={}

	print prefix

	for node in cluster_nodes:
		if (node.state != NodeState.RUNNING):
			continue
		if node.name.startswith (prefix):
			print "[*] %-22s| %s | %s " % (node.name, node.private_ips, node.public_ips)
			node_ip = get_node_ip(node)
			env.hosts += [node_ip]
			env.host_names[node_ip] = node.name
			env.host_roles[node_ip] = get_node_role(node)

	print '\n-----------------------------------\n'

def env_host (node):
	env.seeds=[]
	env.hosts=[]
	env.host_names={}
	env.host_roles={}

	node_ip = get_node_ip(node)

	env.host = node_ip
	env.hosts += [node_ip]
	env.host_names[node_ip] = node.name
	env.host_roles[node_ip] = get_node_role(node)

def get_node_ip (node):
	if node.public_ips:
		return node.public_ips[0]
	else:
		return node.private_ips[0]

def get_node_role (node):
	if node.name.startswith (FRONTEND_NAME_PREFIX):
		return "frontend"
	elif node.name.startswith (MASTER_NAME_PREFIX):
		return "master"
	elif node.name.startswith (WORKER_NAME_PREFIX):
		return "worker"
	else:
		return "other"

@task
def select():

	if env.hosts is None or len(env.hosts) == 0:
		env_hosts('')

	idx = 0
	for host in env.hosts:
		idx += 1
		print "[%s] %-22s| %s " % (idx, env.host_names[host], host)

	if idx > 1:
		host_idx = int(prompt('Please chose host: '))
	else:
		host_idx = 1

	host = env.hosts[host_idx-1]
	print host

	env.host = host
	env.hosts = [host]
	env.host_string = host

def seeds ():
	cluster_nodes = get_nodes(prefix=MASTER_NAME_PREFIX)
	ips = []
	for node in cluster_nodes:
		node_ip = node.private_ips[0]
		ips += [node_ip]

	return ips

def compose (seeds):
	seeds_str = '\\[';
	for s in seeds:
		seeds_str += '\\\"'+s+":2551"+'\\\",'
	seeds_str = seeds_str[:-1]+']'
	return seeds_str



##################################################################################################
#
# Node Provisioning
#
NEWRELIC_KEY = '6284d11c85a01a95742c3aaff1259119d10fdc90'

@task
@parallel
def provision ():
	conf_hostfile()
	#install_java8()
	#install_newrelic()

def conf_hostfile ():
	sudo ("echo %(host)s `hostname` localhost >> /etc/hosts" % env)
	#sudo ("echo %(host)s localhost >> /etc/hosts" % env)

def install_newrelic():
	print "Installing newreilc"
	sudo ('echo deb http://apt.newrelic.com/debian/ newrelic non-free >> /etc/apt/sources.list.d/newrelic.list')
	sudo ('wget -O- https://download.newrelic.com/548C16BF.gpg | apt-key add -')
	sudo ('apt-get update')
	sudo ('apt-get install newrelic-sysmond')
	sudo ('nrsysmond-config --set license_key='+NEWRELIC_KEY)
	sudo ('/etc/init.d/newrelic-sysmond start')

	print 'Installing newrelic... DONE'


def install_java8 ():
	print "Installing Java8"
	sudo ('add-apt-repository -y ppa:webupd8team/java')
	sudo ('apt-get update')
	# auto agree to the Oracle license
	sudo ('echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections')
	sudo('echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections')
	# install oracle-java7
	sudo ('apt-get install -y oracle-java8-installer')
	print 'Installing Java8... DONE'


###########################################################################
#
# Cluster manage
#
@task
def destroy_cluster ():
	print 'Destroying '+CLUSTER_NAME_PREFIX+' cluster'

	cluster_nodes = get_nodes(prefix=CLUSTER_NAME_PREFIX)

	for node in cluster_nodes:
		print 'Destroying %s...' % node.name
		node.destroy()

@task
def reboot_cluster ():
	print 'Reboot '+CLUSTER_NAME_PREFIX+' cluster'

	cluster_nodes = get_nodes(prefix=CLUSTER_NAME_PREFIX)

	for node in cluster_nodes:
		print 'Restarting %s...' % node.name
		node.reboot()


@task
def create_cluster (frontends=1, masters=1, workers=2):
	print 'Creating '+CLUSTER_NAME_PREFIX+' cluster'
	create_masters(int(masters))
	create_workers(int(workers))
	create_frontends(int(frontends))
	wait_for_running_state()

@task
def create_worker_image():
	nodes = get_nodes(prefix=WORKER_NAME_PREFIX)

	if len(nodes) == 0:
		print "No worker nodes available for prefix - "+prefix
		return

	create_image(nodes[0], wrapperself.get_property('SCALING_WORKER_IMAGE'))


def get_nodes (prefix=CLUSTER_NAME_PREFIX, refetch=False):
	global nodes
	if nodes == None or refetch:
		nodes = driver.list_nodes()
		nodes = sorted (nodes, key=lambda node: node.name)

	nodes_res = [node for node in nodes if node.name.startswith(prefix) and node.state == NodeState.RUNNING]
	return nodes_res

def get_image(name):
	global images
	if images == None:
		images = driver.list_images()
	return next ((img for img in images if img.name==name), None)

def get_size (name):
	global sizes
	if sizes == None:
		sizes = driver.list_sizes()
	return next ((size for size in sizes if size.id==name or size.name==name), None)

def create_masters(size_masters):
	for idx in range (0,size_masters):
		create_master(MASTER_NAME_PREFIX+`idx+1`)

def create_workers(size_workers):
	for idx in range (0,size_workers):
		create_worker(WORKER_NAME_PREFIX+`idx+1`)

def create_frontends(size_frontend):
	for idx in range (0,size_frontend):
		create_frontend(FRONTEND_NAME_PREFIX+`idx+1`)

def create_master(name):
	image = get_image(wrapper.get_property('DEFAULT_IMAGE'))
	size = get_size(wrapper.get_property('MASTER_SIZE_NAME'))
	create_node(name, image, size)

def create_worker(name):
	image = get_image(wrapper.get_property('DEFAULT_IMAGE'))
	size = get_size(wrapper.get_property('WORKER_SIZE_NAME'))
	create_node(name, image, size)

def create_frontend(name):
	image = get_image(wrapper.get_property('DEFAULT_IMAGE'))
	size = get_size(wrapper.get_property('FRONTEND_SIZE_NAME'))
	create_node(name, image, size)

def create_node (name, image, size):
	print "Creating node >> [%s , %s , %s]" % (name, image, size)
	res = driver.create_node(name=name, image=image, size=size, ex_keyname=wrapper.get_property('ex_keyname'))

def create_image (node, name):
	print "Creating snapshot node >> [%s , %s]" % (name, node.name)
	res = driver.create_image(node, name)


def wait_for_running_state():
	import sys
	from time import sleep
	print 'Waiting for cluster to be initialized'
	while True:
		nodes = driver.list_nodes()
		cluster_nodes = [node for node in nodes if node.name.startswith (CLUSTER_NAME_PREFIX) and node.state == NodeState.PENDING]

		if len(cluster_nodes) == 0:
			break
		else:
			sys.stdout.write ('.')
			sys.stdout.flush()
			sleep(0.1)

	print ''
	print 'Cluster up and running!:)'


def get_unique_node_name (prefix):
	env_hosts(prefix)

	idx = 0
	while True:
		idx += 1
		name = prefix + `idx`

		if not name in env.host_names.values():
			break


	return name



# # # # # # # # # # #
# Initialize driver
#
from drivers import *
def initialize ():
	global wrapper
	wrapper = get_wrapper()
	wrapper.initialize()
	global driver
	driver = wrapper.get_driver()

	env.user = wrapper.get_property('env_user')
	env.password = wrapper.get_property('env_password')
	env.gateway = wrapper.get_property('env_gateway')
	env.key_filename = wrapper.get_property('env_key_filename')
