module "observability" {
  source              = "../../modules/observability"
  namespace           = "observability-staging"
  release_name        = "hermes-observability-staging"
  chart_path          = "../../../helm/hermes-observability"
  values_file         = "../../../helm/environments/staging-values.yaml"
  alertmanager_secrets = {
    ALERTMANAGER_SLACK_API_URL       = var.alertmanager_slack_api_url
    ALERTMANAGER_SLACK_CHANNEL       = var.alertmanager_slack_channel
    ALERTMANAGER_PAGERDUTY_ROUTING_KEY = var.alertmanager_pagerduty_routing_key
    ALERTMANAGER_EMAIL_TO            = var.alertmanager_email_to
    ALERTMANAGER_EMAIL_FROM          = var.alertmanager_email_from
    ALERTMANAGER_SMARTHOST           = var.alertmanager_smarthost
    ALERTMANAGER_SMTP_USER           = var.alertmanager_smtp_user
    ALERTMANAGER_SMTP_PASSWORD       = var.alertmanager_smtp_password
  }
}
