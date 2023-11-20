require 'csv'
require 'json'

idx = 0
CSV.foreach("data/behandlings_aktiviteter.csv", headers: true, col_sep: ';', encoding: 'utf-8') do |row|
  p6value = row[2]
  p7value = row[3]
  p8value = row[4]
  p9value = row[5]
  p10value = row[6]
  p11value = row[7]
  p12value = row[8]
  p13value = row[9]
  p14value = row[10]
  choices = []
  if !p6value.nil? && p6value.strip.length > 0
    choices <<= "register-gdpr-valp6"
    p6value.split(";").each do |choice|
      choices <<= "register-gdpr-p6-#{choice.strip}"
    end
  end
  if !p7value.nil? && p7value.strip.length > 0
    choices <<= "register-gdpr-valp7"
    p7value.split(";").each do |choice|
      choices <<= "register-gdpr-p7-#{choice.strip}"
    end
  end
  choices <<= "register-gdpr-valp8" if !p8value.nil? && p8value.strip.length > 0
  choices <<= "register-gdpr-valp9" if !p9value.nil? && p9value.strip.length > 0
  choices <<= "register-gdpr-valp10" if !p10value.nil? && p10value.strip.length > 0
  choices <<= "register-gdpr-valp11" if !p11value.nil? && p11value.strip.length > 0
  choices <<= "register-gdpr-valp12" if !p12value.nil? && p12value.strip.length > 0
  choices <<= "register-gdpr-valp13" if !p13value.nil? && p13value.strip.length > 0
  choices <<= "register-gdpr-valp14" if !p14value.nil? && p14value.strip.length > 0
  register = {
    packageName: 'kl_article30',
    name: row[0],
    description: row["Beskrivelse af behandlingen"].strip,
    gdprChoices: choices
  }
  filename = sprintf("./kl_article30_%02d.json", idx)
  File.write(filename, JSON.dump(register))
  idx += 1
end