require 'csv'
require 'json'

kle_main_map = Hash.new
kle_group_map = Hash.new
CSV.foreach("kle-mapping.csv", headers: true, col_sep: ';', encoding: 'utf-8') do |row|
  key = "#{row[0]}. #{row[1]}".gsub(/[\r\n]/, "")

  kle_main_map[key] = [] unless kle_main_map.key?(key)
  kle_main_map[key] = kle_main_map[key] + [row[2][0..1]]
  kle_main_map[key] = kle_main_map[key].uniq

  kle_group_map[key] = [] unless kle_group_map.key?(key)
  kle_group_map[key] = kle_group_map[key] + [row[3][0..5].gsub(/[ ]/, "")]
  kle_group_map[key] = kle_group_map[key].uniq

end

Dir.glob("../src/main/resources/data/registers/*.json").each do |path|
  data = JSON.parse(File.read(path))
  name = data["name"]
  puts "#{name} Have no KLE" unless kle_main_map.has_key?(name)
  data['kleMainGroups'] = kle_main_map[name]
  data['kleGroups'] = kle_group_map[name]
  File.open(path, "w") do |f|
    f.write(JSON.pretty_generate(data))
  end
end
