locals {
  nginx_vars = {
    app_host    = "127.0.0.1"
    app_port    = var.app_port
    listen_port = var.proxy_port
  }

  app_container_vars   = [for k, v in var.env_vars : { name = upper(k), value = v }]
  nginx_container_vars = [for k, v in local.nginx_vars : { name = upper(k), value = v }]

  container_definitions = (
    var.nginx_proxy ?
    merge(local.app_container_definition, local.nginx_container_definition) :
    local.app_container_definition
  )

  app_container_definition = {
    app = {
      name = var.app_container_name
      image = (
        startswith(var.image, "sha256") ?
        "${var.ecr_registry}/${var.ecr_repository}@${var.image}" :
        "${var.ecr_registry}/${var.ecr_repository}:${var.image}"
      )
      health_check = length(var.app_health_check) > 0 ? {
        command = split(",", var.app_health_check)
      } : {}
      readonly_root_filesystem = false
      essential                = true
      memory_reservation       = 256
      environment              = local.app_container_vars
      port_mappings = [
        {
          name          = var.app_container_name
          containerPort = var.app_port
          hostPort      = var.app_port
        }
      ]
      mount_points = [
        {
          readOnly      = false
          containerPath = "/vol/web"
          sourceVolume  = "static"
        }
      ]
    }
  }
  nginx_container_definition = {
    nginx = {
      name  = "nginx"
      image = "${var.ecr_registry}/nginx-proxy:latest"
      health_check = {
        command = ["CMD-SHELL", "curl -so /dev/null http://localhost/health || exit 1"]
      }
      readonly_root_filesystem = false
      essential                = true
      memory_reservation       = 256
      environment              = local.nginx_container_vars
      port_mappings = [
        {
          name          = "nginx"
          containerPort = var.proxy_port
          hostPort      = var.proxy_port
        }
      ]
      mount_points = [
        {
          readOnly      = false
          containerPath = "/vol/static"
          sourceVolume  = "static"
        }
      ]
    }
  }
}

resource "null_resource" "cluster_arn_precondition_check" {
  lifecycle {
    precondition {
      condition     = (var.create_cluster == false && var.cluster_arn != "" || var.create_cluster && var.cluster_arn == "")
      error_message = "The cluster ARN must be provided if 'create_cluster' is false. If you mean to have this module create the cluster, set 'create_cluster' to true."
    }
  }
}

module "service" {
  source  = "terraform-aws-modules/ecs/aws//modules/service"
  version = "~> 5.7.0"

  name        = "${var.app_name}-${var.environment}"
  cluster_arn = var.create_cluster ? module.cluster.arn : var.cluster_arn
  capacity_provider_strategy = {
    env_strategy = {
      base              = 0
      capacity_provider = var.environment == "production" ? "FARGATE" : "FARGATE_SPOT"
      weight            = 100
    }
  }

  # allow ECS exec commands on containers (e.g. to get a shell session)
  enable_execute_command = true

  # resources
  cpu    = var.cpu
  memory = var.memory

  # do not force a new deployment unless the image digest has changed
  force_new_deployment = false

  # wait for service to reach steady state
  wait_for_steady_state = true

  # wait for the task to reach steady state
  wait_until_stable = true

  # Container definition(s)
  container_definitions = local.container_definitions

  deployment_circuit_breaker = {
    enable   = true
    rollback = true
  }

  load_balancer = {
    service = {
      target_group_arn = aws_lb_target_group.app.arn
      container_name   = var.nginx_proxy ? "nginx" : "app"
      container_port   = var.nginx_proxy ? var.proxy_port : var.app_port
    }
  }

  subnet_ids = local.private_subnets

  security_group_rules = {
    ingress_vpc = {
      type        = "ingress"
      from_port   = var.nginx_proxy ? var.proxy_port : var.app_port
      to_port     = var.nginx_proxy ? var.proxy_port : var.app_port
      protocol    = "tcp"
      cidr_blocks = [local.vpc_cidr]
    }
    egress_all = {
      type        = "egress"
      from_port   = 0
      to_port     = 0
      protocol    = "-1"
      cidr_blocks = ["0.0.0.0/0"]
    }
  }

  tasks_iam_role_statements = var.iam_statements

  timeouts = {
    create = "10m"
    update = "10m"
  }

  volume = {
    static = {}
  }
}
