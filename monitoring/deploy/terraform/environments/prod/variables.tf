variable "alertmanager_slack_api_url" { type = string, sensitive = true }
variable "alertmanager_slack_channel" { type = string }
variable "alertmanager_pagerduty_routing_key" { type = string, sensitive = true }
variable "alertmanager_email_to" { type = string }
variable "alertmanager_email_from" { type = string }
variable "alertmanager_smarthost" { type = string }
variable "alertmanager_smtp_user" { type = string, sensitive = true }
variable "alertmanager_smtp_password" { type = string, sensitive = true }
