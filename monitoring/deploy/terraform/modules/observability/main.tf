terraform {
  required_providers {
    helm = {
      source  = "hashicorp/helm"
      version = ">= 2.12.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 2.27.0"
    }
  }
}

variable "namespace" { type = string }
variable "release_name" { type = string }
variable "chart_path" { type = string }
variable "values_file" { type = string }
variable "alertmanager_secrets" {
  type      = map(string)
  sensitive = true
}

resource "kubernetes_secret" "alertmanager" {
  metadata {
    name      = "hermes-alertmanager-secrets"
    namespace = var.namespace
  }
  data = var.alertmanager_secrets
  type = "Opaque"
}

resource "helm_release" "observability" {
  name             = var.release_name
  chart            = var.chart_path
  namespace        = var.namespace
  create_namespace = true
  values           = [file(var.values_file)]

  depends_on = [kubernetes_secret.alertmanager]
}
