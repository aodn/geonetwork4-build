variable "alb_parameter_name" {
  description = "The parameter name to derive the ALB details from."
  type        = string
}

variable "app_container_name" {
  description = "The name of the primary application container"
  type        = string
  default     = "app"
}

variable "app_health_check" {
  description = "The health check commmand to run on the docker container."
  type        = string
  default     = null
}

variable "app_hostnames" {
  description = "Hostnames to associate with the application"
  type        = list(string)
}

variable "app_name" {
  description = "The name of the application e.g. sample-django-app"
  type        = string
}

variable "app_port" {
  description = "The port to expose to the nginx proxy on the application container."
  type        = number
  default     = 9000
}

variable "cluster_arn" {
  description = "ARN of the existing cluster to deploy the service/tasks to."
  type        = string
  default     = ""
}

variable "cpu" {
  description = "The CPU capacity to allocate to the task."
  type        = number
  default     = 512
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

variable "env_vars" {
  description = "Map of key/pair values to pass to the container definition."
  type        = map(any)
}

variable "environment" {
  description = "Environment name to prepend/append to resource names"
  type        = string
}

variable "iam_statements" {
  description = "List of IAM statements to attach to the task role"
  type        = any
  default     = []
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
