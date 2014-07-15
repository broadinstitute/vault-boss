template.project = 'boss'

template.elastic_ip = case environment
when 'ci'
  '23.21.61.64'
when 'production'
  '23.21.61.64'
end

template.service_port = "8009"
