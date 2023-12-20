locals {
  environment      = try(yamldecode(file("..//vars/${local.environment_name}/environment.yaml")))
  environment_name = get_env("ENVIRONMENT")
  state_bucket     = "tfstate-${local.environment.aws_account_id}-${local.environment.aws_region}"
  state_key        = "apps/${local.environment.app_name}/${local.environment_name}/${basename(get_terragrunt_dir())}.tfstate"
}

generate "providers" {
  path      = "providers.tf"
  if_exists = "overwrite_terragrunt"
  contents  = <<EOF
provider "aws" {
  region              = "${local.environment.aws_region}"
  allowed_account_ids = ["${local.environment.aws_account_id}"]
  default_tags {
    tags = {
      "Environment" = "apps"
      "ManagedBy" = "Apps - ${local.state_bucket}/${local.state_key}"
      "Owner" = "Platform Engineering"
      "Project" = "AODN Applications"
      "Repository" = "aodn/${local.environment.app_name}"
    }
  }
}
EOF
}

remote_state {
  backend = "s3"
  generate = {
    path      = "backend.tf"
    if_exists = "overwrite_terragrunt"
  }
  config = {
    bucket                      = local.state_bucket
    key                         = local.state_key
    region                      = local.environment.aws_region
    dynamodb_table              = local.state_bucket
    skip_credentials_validation = true
    skip_metadata_api_check     = true
    skip_region_validation      = true
    disable_bucket_update       = true
    encrypt                     = true
  }
}
