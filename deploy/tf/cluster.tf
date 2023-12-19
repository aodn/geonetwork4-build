module "cluster" {
  source  = "terraform-aws-modules/ecs/aws//modules/cluster"
  version = "~> 5.7.0"

  create = var.create_cluster ? true : false

  # Cluster Configuration
  cluster_name = "${var.app_name}-${var.environment}"
  cluster_configuration = {
    name  = "containerInsights"
    value = "enabled"
  }
  create_task_exec_iam_role = true
  fargate_capacity_providers = {
    FARGATE = {
      default_capacity_provider_strategy = {
        weight = 50
      }
    }
    FARGATE_SPOT = {
      default_capacity_provider_strategy = {
        weight = 50
      }
    }
  }
}
