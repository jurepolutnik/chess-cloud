from fabric.api import *
from fabric.contrib.console import confirm

LOCAL_CLUSTER_APP_LOCATION = '../cluster/'
LOCAL_FRONTEND_APP_LOCATION = '../frontend/'
LOCAL_FOLDER_DIST = "dist"

REMOTE_SERVICE_FRONTEND = "chesscloud-frontend"
REMOTE_SERVICE_CLUSTER  = "chesscloud-cluster"
REMOTE_LOCATION_FRONTEND = '/opt/'+REMOTE_SERVICE_FRONTEND
REMOTE_LOCATION_CLUSTER  = '/opt/'+REMOTE_SERVICE_CLUSTER
REMOTE_LOG_LOCATION  	= '/var/log/upstart/'


LOCAL_CLUSTER_CONF_LOCATION = LOCAL_CLUSTER_APP_LOCATION + "src/universal/conf/"
LOCAL_FRONTEND_CONF_LOCATION = LOCAL_FRONTEND_APP_LOCATION + "conf/"
FILE_APPLICATION_INI = 'application.ini'
FILE_SEEDS_CONFIG = 'seeds.conf'

SEEDS_PLACEHOLDER  = "\\<SEEDS_PLACEHOLDER\\>"
CLUSTER_ROLE_PLACEHOLDER = "\\<CLUSTER_ROLE_PLACEHOLDER\\>"

env.path = "~"


##################################################################################################
# Build / Deploy
#
@task
def package(role):
	env.role = role
	env.bundle_name = 'chesscloud-cluster-'+role
	env.bundle_tgz = env.bundle_name + '.tar.gz'

	if (role != "frontend"):
		app_location 	= LOCAL_CLUSTER_APP_LOCATION
		application_ini = LOCAL_CLUSTER_CONF_LOCATION + FILE_APPLICATION_INI
		seeds_conf 		= LOCAL_CLUSTER_CONF_LOCATION + FILE_SEEDS_CONFIG
	else:
		app_location 	= LOCAL_FRONTEND_APP_LOCATION
		application_ini = LOCAL_FRONTEND_CONF_LOCATION + FILE_APPLICATION_INI
		seeds_conf 		= LOCAL_FRONTEND_CONF_LOCATION + FILE_SEEDS_CONFIG

	env.debfile = "chesscloud-cluster-%s.deb" % role

	try:
		### PREPARE ###
		SED_CMD = 'sed -i s/%s/%s/g %s'
		local ("cp %s %s" % (seeds_conf, seeds_conf+".orig"))
		local (SED_CMD % (SEEDS_PLACEHOLDER, compose(seeds()), seeds_conf))

		if (role != "frontend"):
			local ("cp %s %s" % (application_ini, application_ini+".orig"))
			local (SED_CMD % (CLUSTER_ROLE_PLACEHOLDER, role, application_ini))


		### BUILD ###
		local ('cd %s && rm -rf target/ && exec sbt clean debian:packageBin' % app_location)

		local ('mkdir -p %s' % LOCAL_FOLDER_DIST)
		local ('cp %s/target/chesscloud-*.deb %s/%s' %
			(app_location, LOCAL_FOLDER_DIST, env.debfile))

	finally:
		### CLENAUP ###
		local ("mv %s %s" % (seeds_conf+".orig", seeds_conf))
		if (role != "frontend"):
			local ("mv %s %s" % (application_ini+".orig", application_ini))

@task
@parallel
def deploy():
	role = env.host_roles[env.host]
	env.debfile = "chesscloud-cluster-%s.deb" % role

	with settings(warn_only=True):
		run ('mv %(debfile)s %(debfile)s_old' % env)

	put (("%s/%s" % (LOCAL_FOLDER_DIST, env.debfile)), ("."))
	sudo ("dpkg -i --force-all %(debfile)s" % env)



@task
@parallel
def install():
	role = env.host_roles[env.host]
	env.debfile = "chesscloud-cluster-%s.deb" % role
	put (("%s/%s" % (LOCAL_FOLDER_DIST, env.debfile)), ("."))
	sudo ("dpkg -i --force-all %(debfile)s" % env)

@task
@parallel
def remove():
	role = env.host_roles[env.host]
	env.service = remote_service()
	sudo ("dpkg -r --force-all %(service)s" % env)

@task
@parallel
def rollback():
	remove()
	role = env.host_roles[env.host]
	env.debfile = "chesscloud-cluster-%s.deb" % role
	run ('mv %(debfile)s %(debfile)s_temp' % env)
	run ('mv %(debfile)s_old %(debfile)s' % env)
	run ('mv %(debfile)s_temp %(debfile)s_old' % env)
	install()


@task
@parallel
def start ():
	env.service = remote_service()

	sudo ('rm -f /var/run/%(service)s/RUNNING_PID' % env)
	with settings(warn_only=True):
		sudo ('service %(service)s start' % env)

@task
@parallel
def stop ():
	env.service = remote_service()
	with settings(warn_only=True):
		sudo ('service %(service)s stop' % env)

@task
def deploy_cluster():
	deploy_masters()
	deploy_workers()
	deploy_frontends()

@task
def deploy_masters():
	package("master")
	masters()
	execute(remove)
	execute(deploy)

@task
def deploy_workers():
	package("worker")
	workers()
	execute(remove)
	execute(deploy)

@task
def deploy_frontends():
	package("frontend")
	frontends()
	execute (remove)
	execute(deploy)

@task
def stop_cluster():
	cluster()
	execute (stop)

@task
def start_cluster():
	print "Starting masters..." % env
	masters()
	execute(start)
	print "Starting workers..." % env
	workers()
	execute(start)
	print "Starting frontends..." % env
	frontends()
	execute(start)

def remote_service():
	role = env.host_roles[env.host]
	if (role == "frontend"):
		return REMOTE_SERVICE_FRONTEND
	else:
		return REMOTE_SERVICE_CLUSTER


##################################################################################################
# Manage
#
@task
@parallel
def get_log ():
	get("%s/chesscloud-*.log" % REMOTE_LOG_LOCATION, local_path='logs/%s.log' % env.host_names[env.host])

@task
@parallel
def clean_log ():
	sudo("rm %s/chesscloud-*.log" % REMOTE_LOG_LOCATION)


@task
def tail_log ():
	select()
	sudo("tail -f %s/chesscloud-*.log" % REMOTE_LOG_LOCATION)



##################################################################################################
# Scaling
#
@task
def scale_up (cluster_role='worker'):

	print 'Creating new ' + cluster_role + '\n'

	if (cluster_role == 'frontend'):
		name = get_unique_node_name(FRONTEND_NAME_PREFIX)
		create_frontend(name)
	elif (cluster_role == 'master'):
		name = get_unique_node_name(MASTER_NAME_PREFIX)
		create_master(name)
	elif (cluster_role == 'worker'):
		name = get_unique_node_name(WORKER_NAME_PREFIX)
		create_worker(name)

	print '\nWaiting for (running state)' + name
	wait_for_running_state()

	node = get_nodes (name, refetch=True)[0]
	env_host(node)

	print '\nWaiting for (ssh) ' + name + '\n'
	execute(wait_for_ssh)

	# Not needed in AWS
	#print '\nProvisioning ' + name + '\n'
	#execute(provision)

	print '\nDeploying ' + name + '\n'
	execute(deploy)



@task
def scale_down(cluster_role='worker'):

	print 'Scaling down ' + cluster_role +'s ...'

	if (cluster_role == 'frontend'):
		nodes = get_nodes(FRONTEND_NAME_PREFIX)
	elif (cluster_role == 'master'):
		nodes = get_nodes(MASTER_NAME_PREFIX)
	elif (cluster_role == 'worker'):
		nodes = get_nodes(WORKER_NAME_PREFIX)

	if len(nodes) > 1:
		node = nodes[-1]
		env_host(node)
		print '\nStoping %s ... \n' % node.name
		execute(stop)
		print '\nDestroying %s ...\n' % node.name
		node.destroy()


@task
def wait_for_ssh():
	import sys
	from time import sleep
	print 'Waiting for node to be accessable (ssh)'

	while True:
		try:
			run('pwd')
			break
		except:
			pass

		sys.stdout.write ('.')
		sys.stdout.flush()
		sleep(1)


	print ''
	print 'Node is accessible...'



from infrastructure import *
initialize()
