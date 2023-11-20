require 'csv'
require 'json'

parents = []
children = []
idx = 0
CSV.foreach("data/iso27002_2017.csv", headers: true, col_sep: ';', encoding: 'utf-8') do |row|
  heading = row[0].split(' ')
  parent_heading = heading[1]
  area = row[1].split(' ')
  parent_sub_section = area[0]
  parent_sub_heading = area[1]

  parent_sub_section_digits = parent_sub_section.gsub(/[^0-9]/, '')
  parent_identifier = sprintf("iso_27002_2017_%03d", parent_sub_section_digits.to_i)
  parents[parent_sub_section_digits.to_i] = {
    standardIdentifier: "iso27002_2017",
    identifier: parent_identifier,
    section: parent_sub_section,
    description: "#{parent_heading} - #{parent_sub_heading}",
    sortKey: parent_sub_section_digits.to_i
  }

  section = row[2]
  section_digits = section.gsub(/[^0-9]/, '')
  children[idx] = {
    parentIdentifier: parent_identifier,
    identifier: "iso27002_2017_#{section_digits}",
    section: section,
    description: row[3],
    sortKey: idx
  }
  idx+=1
end
all_sections = parents.compact + children.compact
File.write("./iso27002_2017_sections.json", JSON.dump(all_sections))
