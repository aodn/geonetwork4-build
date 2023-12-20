# ssm variables
variable "alb_parameter_name" {
  description = "The parameter name to derive the ALB details from."
  type        = string
}

# task exec role
variable "task_exec_iam_statements" {
  description = "A map of IAM policy [statements](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document#statement) for custom permission usage"
  type        = any
  default     = {}
}

# tasks role
variable "tasks_iam_role_statements" {
  description = "A map of IAM policy [statements](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document#statement) for custom permission usage"
  type        = any
  default     = {}
}

# container variables
variable "app_container_name" {
  description = "The name of the primary application container"
  type        = string
  default     = "app"
}

variable "app_health_check" {
  description = "The health check command to run on the docker container."
  type        = string
  default     = ""
}

variable "app_port" {
  description = "The port to expose on the application container."
  type        = number
  default     = 9000
}

variable "container_secrets" {
  description = "Map of environment variables and secrets to retrieve values from."
  type = list(object({
    name      = string
    valueFrom = string
  }))
  default = null
}

variable "cpu" {
  description = "The CPU capacity to allocate to the task."
  type        = number
  default     = 512
}

variable "env_vars" {
  description = "List of key/pair values to pass to the container definition."
  type = list(object({
    value = string
    type  = string
  }))
  default = []
}

variable "environment_files" {
  description = "A list of files containing the environment variables to pass to a container"
  type = list(object({
    value = string
    type  = string
  }))
  default = []
}

variable "image" {
  description = "The digest/tag of the docker image to pull from ECR"
  type        = string
  default     = "latest"
}

variable "memory" {
  description = "The CPU capacity to allocate to the task."
  type        = number
  default     = 1024
}

variable "nginx_proxy" {
  description = "Whether or not to side-load an nginx container in the task definition"
  type        = bool
  default     = true
}

variable "proxy_port" {
  description = "The port to expose to the load balancer on the container"
  type        = number
  default     = 80
}

# general variables
variable "app_hostnames" {
  description = "Hostnames to associate with the application"
  type        = list(string)
}

variable "app_name" {
  description = "The name of the application e.g. sample-django-app"
  type        = string
}

variable "cluster_arn" {
  description = "ARN of the existing cluster to deploy the service/tasks to."
  type        = string
  default     = ""
}

variable "create_cluster" {
  description = "Whether or not to create a separate cluster for this deployment. If false, the name of an existing cluster must be provided."
  type        = bool
  default     = true
}

variable "ecr_registry" {
  description = "The registry to pull docker images from."
  type        = string
}

variable "ecr_repository" {
  description = "The repository to pull the image from."
  type        = string
}

variable "environment" {
  description = "Environment name to prepend/append to resource names"
  type        = string
}

# Target group health checks
variable "health_check_path" {
  description = "The health check path for the ALB target group."
  type        = string
  default     = "/health"
}

variable "healthy_threshold" {
  description = "Number of consecutive health check successes required before considering a target healthy. The range is 2-10."
  type        = number
  default     = null
}

variable "interval" {
  description = "The amount of time in seconds between health checks."
  type        = number
  default     = null
}

variable "unhealthy_threshold" {
  description = "Number of consecutive health check failures required before considering a target unhealthy. The range is 2-10."
  type        = number
  default     = null
}
