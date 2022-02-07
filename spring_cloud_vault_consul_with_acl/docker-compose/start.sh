#!/bin/bash

printf "Setting up local environment.\n"

# start containers
docker-compose up -d

# pause for a bit before containers come up
read -t 10 -p "waiting for 10 secs before we get you setup  ..."

printf "\n"

## CONFIG LOCAL ENV
alias consul='docker-compose exec consul consul "$@"'
export CONSUL_ADDR=http://0.0.0.0:8500

# set consul KV for local profile
consul kv put local/examples/spring-boot-example/application.yaml "$(cat ./../config/local/application.yaml)"

# setup consul policy
consul acl policy create -name read-only -rules "key_prefix \"\" {  policy = \"read\"}" -token=foo -http-addr=$CONSUL_ADDR

# alias for localstack
# alias awslocal='docker-compose exec localstack "$@"'

# create a s3 bucket
aws --endpoint-url=http://localhost:4566 s3 mb s3://example-s3

# copy sample object to s3 for testing
aws --endpoint-url=http://localhost:4566 s3 cp text-test.txt s3://example-s3/test-data/

# test s3 integration
aws --endpoint-url=http://localhost:4566 s3 ls s3://example-s3/test-data/

# alias for vault
alias vault='docker-compose exec vault vault "$@"'

# authenticate against vault
export VAULT_TOKEN="foo"
vault login foo

# vault populate kv
vault kv put secret/local/examples/spring-boot-example password=password1

# enable consul secret engine
vault secrets enable consul

# setup consul backend config access
vault write consul/config/access address=http://consul:8500 token=foo

# setup consul policy
vault write consul/roles/consul-read-only policies=read-only

# test vault and consul connectivity: read consul policy
vault read consul/creds/consul-read-only

# enable aws secret backend
vault secrets enable aws

# configure aws access for vault aws backend with custom iam and sts endpoints
vault write aws/config/root access_key=test secret_key=test region=us-east-1 iam_endpoint=http://localstack:4566 sts_endpoint=http://localstack:4566

# deploy aws sts policy
vault write aws/roles/readonly-examples role_arns=arn:aws:iam::foo:role/readonly-examples credential_type=assumed_role

# test aws sts creds
vault write aws/sts/readonly-examples ttl=60m

printf "\nDone setting up local environment!"
