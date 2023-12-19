#dependency "s3" {
#  config_path  = "../s3"
#  skip_outputs = true
#}

include "global" {
  path   = "../global.hcl"
  expose = true
}

inputs = {
  app_name         = get_env("APP_NAME")
  app_health_check = get_env("APP_HEALTH_CHECK", "")
  cluster_arn      = get_env("CLUSTER_ARN", "")
  create_cluster   = get_env("CREATE_CLUSTER", true)
  environment      = local.global.environment

  # fetch the shared infrastructure parameter name
  alb_parameter_name = get_env("ALB_PARAMETER_NAME")

  # DNS hostnames to associate with the container
  app_hostnames = split(",", get_env("APP_HOSTNAMES", local.default_hostname))

  # container-specific environment variables
  env_vars = local.env_vars

  ecr_registry   = get_env("ECR_REGISTRY")
  ecr_repository = get_env("ECR_REPOSITORY")

  iam_statements = local.iam_statements
}

locals {
  global = include.global.locals

  # container/task environment variables
  default_env_vars = { for tuple in regexall("(.*?)=(.*)", file("../../container/default.env")) : tuple[0] => tuple[1] }

  # get any overrides from the environment (e.g. GitHub deployment variables)
  override_env_vars = {
    for k, v in local.default_env_vars :
    k => can(get_env(k)) ? get_env(k) : v
  }

  # remove empty values from the override map
  env_vars = {
    for k, v in local.override_env_vars : k => v if v != ""
  }

  default_hostname = join("-", [get_env("APP_NAME"), local.global.environment])

  iam_statements = try(yamldecode(templatefile("../..//iam_statements/${local.global.environment}.yaml.tftpl",
    {
      aws_account = local.global.aws_account
      aws_region  = local.global.aws_region
      environment = local.global.environment
  })), [])
}

terraform {
  source = "../..//tf"
}
