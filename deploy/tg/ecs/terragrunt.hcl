locals {
  environment      = try(yamldecode(file("../..//vars/${local.environment_name}/environment.yaml")))
  environment_name = get_env("ENVIRONMENT")
  vars             = try(yamldecode(file("../..//vars/${local.environment_name}/variables.yaml")))
}

include "global" {
  path = "../global.hcl"
}

inputs = merge(local.vars, {
  app_name    = local.environment.app_name
  environment = local.environment_name

  ecr_registry   = get_env("ECR_REGISTRY")
  ecr_repository = get_env("ECR_REPOSITORY")
})

terraform {
  source = "../..//tf"
}
