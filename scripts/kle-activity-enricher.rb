# frozen_string_literal: true

# Lægger KLE på pakke-JSON'erne ud fra KL's ark. Køres efter activity_importer.rb,
# som laver filerne med titel, beskrivelse og GDPR-hjemmel.
#
#   ruby kle-activity-enricher.rb --tørkørsel
#   ruby kle-activity-enricher.rb
#   ruby kle-activity-enricher.rb "../KL-mappede ... version 1.8.xlsx" --ud /tmp/nye-pakker
#
# Bruger fanen "KLE samlet", der indeholder alle tre niveauer. De tre opsplittede
# faner (Hovedgruppe/Gruppe/Emne) er samme rækker fordelt på niveau - verificeret
# identiske - så der er intet at hente ved at læse dem hver for sig.
#
#   B niveau (Hovedgruppe|Gruppe|Emne)   C kode ("00", "00.01", "00.01.00")
#   E nummeret på den behandlingsaktivitet rækken hører til
#
# Koblingen sker på aktivitetsnummer mod fanen "Behandlingsaktiviteter" og derfra
# på titel mod pakkerne, fordi RegisterImporter slår op på name.
require 'json'
require 'set'
require_relative 'xlsx'

KLE_SHEET = 'KLE samlet'
ACTIVITY_SHEET = 'Behandlingsaktiviteter'
LEVELS = { 'Hovedgruppe' => 'kleMainGroups', 'Gruppe' => 'kleGroups', 'Emne' => 'kleSubjects' }.freeze

REGISTERS_DIR = File.join(__dir__, '..', 'src', 'main', 'resources', 'data', 'registers')
EMNEPLAN = File.join(__dir__, '..', 'src', 'main', 'resources', 'data', 'kle-emneplan.xml')

def normalise(text)
  text.to_s.gsub(/\s+/, ' ').strip
end

def locate_workbook(argument)
  return argument if argument

  candidates = Dir.glob(File.join(__dir__, '..', 'KL-mappede*.xlsx'))
  case candidates.size
  when 1 then candidates.first
  when 0 then abort 'Fandt ikke noget KL-ark. Angiv stien som første argument.'
  else abort "Flere ark at vælge mellem, angiv ét:\n  #{candidates.join("\n  ")}"
  end
end

# {aktivitetsnummer => titel}
def titles_by_number(workbook)
  titles = {}
  workbook.rows(ACTIVITY_SHEET) do |rownum, cells|
    titles[cells['B']] = cells['D'] if rownum >= 3 && cells['B'] && cells['D']
  end
  titles
end

# {aktivitetsnummer => {"kleMainGroups" => Set, ...}}
def kle_by_number(workbook)
  kle = {}
  unknown_levels = Set.new
  workbook.rows(KLE_SHEET) do |rownum, cells|
    next if rownum < 2

    level, code, number = cells['B'], cells['C'], cells['E']
    next unless level && code && number

    field = LEVELS[level]
    unknown_levels << level and next unless field

    kle[number] ||= LEVELS.values.to_h { |f| [f, Set.new] }
    kle[number][field] << code
  end
  warn "ukendte niveauer i kolonne B: #{unknown_levels.to_a.join(', ')}" if unknown_levels.any?
  kle
end

# KLE-nummeret bærer sit eget hierarki: emnet 00.07.45 hører under gruppen 00.07 og
# hovedgruppen 00. Arket lister ikke altid forældrene - i v1.8 mangler fx hovedgruppe og
# gruppe for aktivitet 68, og gruppen for aktivitet 71 - og uden denne udfyldning ville
# en opdatering fjerne en forælder og lade emnerne under den stå tilbage uden ophæng.
def complete_hierarchy(kle)
  derived = 0
  kle.each_value do |fields|
    groups = fields['kleSubjects'].filter_map { |s| s[0, 5] if s.match?(/\A\d\d\.\d\d\.\d\d/) }
    mains = (fields['kleSubjects'] + fields['kleGroups']).filter_map { |c| c[0, 2] if c.match?(/\A\d\d/) }
    derived += (groups.to_set - fields['kleGroups']).size + (mains.to_set - fields['kleMainGroups']).size
    fields['kleGroups'].merge(groups)
    fields['kleMainGroups'].merge(mains)
  end
  derived
end

def valid_kle_codes
  return nil unless File.exist?(EMNEPLAN)

  xml = File.read(EMNEPLAN)
  { 'kleMainGroups' => xml.scan(%r{<HovedgruppeNr>([^<]+)</HovedgruppeNr>}).flatten.to_set,
    'kleGroups' => xml.scan(%r{<GruppeNr>([^<]+)</GruppeNr>}).flatten.to_set,
    'kleSubjects' => xml.scan(%r{<EmneNr>([^<]+)</EmneNr>}).flatten.to_set }
end

# Koder appen ikke kender bliver droppet lydløst ved import: KLESubjectService
# slår op på nummer og finder ingenting, uden at det fejler nogen steder.
def report_unknown_codes(kle)
  valid = valid_kle_codes
  return puts "kunne ikke tjekke koder - #{EMNEPLAN} findes ikke" unless valid

  puts "\n-- koder der ikke findes i data/kle-emneplan.xml (tabes ved import) --"
  LEVELS.values.each do |field|
    used = kle.values.reduce(Set.new) { |all, entry| all | entry[field] }
    unknown = (used - valid[field]).to_a.sort
    puts "   #{field}: #{used.size} brugt, #{unknown.size} ukendte#{unknown.any? ? " #{unknown.join(', ')}" : ''}"
  end
end

args = ARGV.dup
dry_run = !args.delete('--tørkørsel').nil? || !args.delete('--dry-run').nil?
out_index = args.index('--ud')
target_dir = out_index ? args.delete_at(out_index + 1) : REGISTERS_DIR
args.delete('--ud')

workbook = Xlsx.new(locate_workbook(args.first))
titles = titles_by_number(workbook)
kle = kle_by_number(workbook)
derived = complete_hierarchy(kle)
puts "aktiviteter med KLE: #{kle.size} af #{titles.size}"
puts "forælder-koder udledt af emne-/gruppenummer (manglede i arket): #{derived}"
missing_kle = titles.keys - kle.keys
puts "uden KLE i arket: #{missing_kle.any? ? missing_kle.join(', ') : 'ingen'}"

unknown_activities = kle.keys - titles.keys
puts "KLE-rækker på ukendt aktivitetsnummer: #{unknown_activities.any? ? unknown_activities.join(', ') : 'ingen'}"
report_unknown_codes(kle)

kle_by_title = kle.to_h { |number, fields| [normalise(titles[number]), fields] }

puts "\n-- pakker i #{target_dir} --"
changed = 0
Dir.glob(File.join(target_dir, '*.json')).sort.each do |path|
  data = JSON.parse(File.read(path))
  title = normalise(data['name'])
  fields = kle_by_title[title]
  if fields.nil?
    puts "   #{File.basename(path)}: #{title[0, 60]} har ingen KLE i arket"
    next
  end

  updated = data.dup
  LEVELS.values.each { |field| updated[field] = fields[field].to_a.sort }
  next if updated == data

  diff = LEVELS.values.filter_map do |field|
    before, after = (data[field] || []).to_set, fields[field]
    "#{field} +#{(after - before).size}/-#{(before - after).size}" if before != after
  end
  puts "   #{File.basename(path)}: #{diff.join(' ')}"
  changed += 1
  File.write(path, "#{JSON.pretty_generate(updated)}\n") unless dry_run
end
puts dry_run ? "\n#{changed} pakker ville blive ændret (tørkørsel)" : "\n#{changed} pakker opdateret"
