require 'csv'
require 'json'

idx = 0
puts "["
CSV.foreach("data/beh.csv", headers: false, col_sep: ',', encoding: 'utf-8') do |row|
  idx = idx + 1
  type = row[2]
  threat = row[3]
  sort_key = row[5]
  name = "%02d" % idx
  puts "{"
  puts "  \"identifier\": \"beh#{name}\","
  puts "  \"sortKey\": #{sort_key},"
  puts "  \"threatCatalogIdentifier\": \"beh\","
  puts "  \"threatType\": \"#{type}\","
  puts "  \"description\": \"#{threat}\","
  puts "  \"rights\": true"
  puts "},"
end
puts "]"
