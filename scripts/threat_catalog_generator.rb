require 'csv'
require 'json'

puts "INSERT INTO threat_catalogs (identifier, name) VALUES ('ishoej_scr', 'Screening');"

idx = 0
CSV.foreach("data/ishoej_threats.csv", headers: true, col_sep: ';', encoding: 'utf-8') do |row|
  idx = idx + 1
  type = row[1]
  threat = row[2]
  name = "%02d" % idx
  puts "INSERT INTO threat_catalog_threats (identifier, thread_catalog_identifier, threat_type, description, rights) VALUES ('ishoej#{name}', 'ishoej_scr', '#{type}', '#{threat}', false);"
end

puts "INSERT INTO threat_catalogs (identifier, name) VALUES ('ishoej_bh', 'Behandlingsaktiviteter');"

idx = 0
CSV.foreach("data/ishoej_threats_bh.csv", headers: true, col_sep: ';', encoding: 'utf-8') do |row|
  idx = idx + 1
  type = row[0]
  threat = row[1]
  name = "%02d" % idx
  puts "INSERT INTO threat_catalog_threats (identifier, thread_catalog_identifier, threat_type, description, rights) VALUES ('ishoej_bh#{name}', 'ishoej_bh', '#{type}', '#{threat}', false);"
end
