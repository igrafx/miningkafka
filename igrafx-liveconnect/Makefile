local_dir=docker-compose/
platform=lc-wk1-demo2
platform_dir=/opt/docker-compose/



test_get:
	rsync -navz --include=".env" --include="docker-compose.yml" --include="nginx-proxy" --exclude-from=rsync-ignore $(platform):$(platform_dir) $(local_dir)

get:
	rsync -avz --include=".env" --include="docker-compose.yml" --include="nginx-proxy" --exclude-from=rsync-ignore $(platform):$(platform_dir) $(local_dir)

test_push:
	rsync -navz --include=".env" --include=".env-non-dev" --exclude-from=rsync-ignore $(local_dir) $(platform):$(platform_dir) 

push:
	rsync -avz --include=".env" --include=".env-non-dev" --exclude-from=rsync-ignore $(local_dir) $(platform):$(platform_dir) 

push_sources:
	rsync -avz --exclude-from=rsync-ignore sources/ $(platform):/opt/sources
