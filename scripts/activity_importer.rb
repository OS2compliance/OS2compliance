# frozen_string_literal: true

# Laver pakke-JSON til src/main/resources/data/registers/ ud fra KL's ark over
# mappede behandlingsaktiviteter.
#
#   ruby activity_importer.rb --rapport
#   ruby activity_importer.rb --ud ../src/main/resources/data/registers
#   ruby activity_importer.rb "../KL-mappede ... version 1.8.xlsx" --ud /tmp/nye-pakker
#
# Læser arket direkte, så der ikke skal eksporteres CSV i hånden først - det var
# der de forkerte kolonner plejede at snige sig ind. KLE lægges oven på af
# kle-activity-enricher.rb, som bruger samme ark.
#
# Fanen "Behandlingsaktiviteter", data fra række 3:
#   A kategori   B nummer   C aktivitet   D titel   E beskrivelse
#   F konsekvens (0-4)   G fortrolighed   H integritet   I tilgængelighed   (X)
#   J lovhjemmel i fritekst - K-AI er den strukturerede udgave af samme:
#   K     Artikel 6, stk. 1   -> register-gdpr-valp6
#   L-Q   litra a-f           -> register-gdpr-p6-a .. -f
#   R     Artikel 9, stk. 2   -> register-gdpr-valp7
#   S-AB  litra a-j           -> register-gdpr-p7-a .. -j
#   AC-AI §8 - §14            -> register-gdpr-valp8 .. valp14
require 'json'
require 'set'
require_relative 'xlsx'

SHEET = 'Behandlingsaktiviteter'
FIRST_DATA_ROW = 3

# KL beholder udgåede aktiviteter i arket og markerer dem i nummeret, fx "81 - UDGÅET".
# De skal ikke blive pakker: en pakke bliver oprettet som fortegnelse hos hver kunde, og en
# fortegnelse med UDGÅET i titlen er ikke noget nogen skal have i sin oversigt. Arket er
# stadig kilden - vi undlader bare at gøre en udgået aktivitet til en fortegnelse.
RETIRED_ACTIVITY = /udgået/i

GDPR_MAIN = { 'K' => 'valp6', 'R' => 'valp7', 'AC' => 'valp8', 'AD' => 'valp9',
              'AE' => 'valp10', 'AF' => 'valp11', 'AG' => 'valp12', 'AH' => 'valp13',
              'AI' => 'valp14' }.freeze
GDPR_P6 = %w[L M N O P Q].zip(('a'..'f').to_a).to_h.freeze
GDPR_P7 = %w[S T U V W X Y Z AA AB].zip(('a'..'j').to_a).to_h.freeze

REGISTERS_DIR = File.join(__dir__, '..', 'src', 'main', 'resources', 'data', 'registers')

def normalise(text)
  text.to_s.gsub(/\s+/, ' ').strip
end

# Pakke reduceret til det der betyder noget, så rækkefølgen i listerne ikke tæller
def comparable(package)
  package.transform_values { |v| v.is_a?(Array) ? v.sort : v }
end

# Finder arket hvis det ikke er givet på kommandolinjen - der ligger normalt kun ét.
def locate_workbook(argument)
  return argument if argument

  candidates = Dir.glob(File.join(__dir__, '..', 'KL-mappede*.xlsx'))
  case candidates.size
  when 1 then candidates.first
  when 0 then abort 'Fandt ikke noget KL-ark. Angiv stien som første argument.'
  else abort "Flere ark at vælge mellem, angiv ét:\n  #{candidates.join("\n  ")}"
  end
end

def read_activities(path)
  activities = []
  Xlsx.new(path).rows(SHEET) do |rownum, cells|
    next if rownum < FIRST_DATA_ROW

    number = cells['B']
    next unless number

    # Titlen er nøglen i RegisterImporter, så en række uden titel kan ikke blive en
    # pakke. Sig det højt frem for at skrive "name": null ned i en fil.
    if cells['D'].nil?
      warn "række #{rownum}: aktivitet #{number} har ingen titel i kolonne D - sprunget over"
      next
    end

    if "#{number} #{cells['D']}".match?(RETIRED_ACTIVITY)
      puts "udgået aktivitet sprunget over: #{number}"
      next
    end

    choices = GDPR_MAIN.filter_map { |col, id| "register-gdpr-#{id}" if cells[col] }
    choices += GDPR_P6.filter_map { |col, letter| "register-gdpr-p6-#{letter}" if cells[col] }
    choices += GDPR_P7.filter_map { |col, letter| "register-gdpr-p7-#{letter}" if cells[col] }

    activities << {
      number: number,
      row: rownum,
      category: cells['A'],
      name: cells['D'],
      description: cells['E'],
      consequence: cells['F'],
      dimensions: { 'fortrolighed' => 'G', 'integritet' => 'H', 'tilgængelighed' => 'I' }
                    .filter_map { |label, col| label if cells[col] },
      gdpr_choices: choices.uniq.sort
    }
  end
  activities
end

def package_for(activity)
  {
    'packageName' => 'kl_article30',
    'name' => activity[:name],
    'description' => activity[:description],
    'gdprChoices' => activity[:gdpr_choices]
  # En tom celle i et fremtidigt ark må ikke kunne nulstille en beskrivelse der
  # allerede står i pakken - så bliver merge i write_packages en sletning
  }.reject { |_, v| v.nil? }
end

# Genbruger filnavnet på en pakke der allerede findes med samme titel, så en
# opdatering giver en læsbar diff i stedet for at renummerere alle filer.
def write_packages(activities, dir)
  Dir.mkdir(dir) unless Dir.exist?(dir)
  existing = {}
  Dir.glob(File.join(dir, 'kl_article30_*.json')).sort.each do |path|
    existing[normalise(JSON.parse(File.read(path))['name'])] = path
  end
  used = existing.values.to_set
  written = 0
  next_index = -1

  activities.sort_by { |a| a[:row] }.each do |activity|
    path = existing[normalise(activity[:name])]
    if path.nil?
      loop do
        next_index += 1
        path = File.join(dir, format('kl_article30_%02d.json', next_index))
        # Både used og File.exist?: en fil der ikke kunne læses som pakke er ikke
        # i used, og må stadig ikke overskrives
        break unless used.include?(path) || File.exist?(path)
      end
      puts "ny fil: #{File.basename(path)}  #{activity[:name][0, 70]}"
    end
    used << path
    current = File.exist?(path) ? JSON.parse(File.read(path)) : nil
    merged = (current || {}).merge(package_for(activity))
    # Sammenlign indhold, ikke tekst: de shippede pakker har gdprChoices i tilfældig
    # rækkefølge, og uden comparable ville hver enkelt fil se ændret ud. Så ville en
    # opdatering give en diff på alle 90+ pakker, og de reelle ændringer ville
    # forsvinde i støjen.
    next if current && comparable(current) == comparable(merged)

    File.write(path, "#{JSON.pretty_generate(merged)}\n")
    written += 1
  end
  puts "skrev #{written} af #{activities.size} pakker til #{dir}"
end

def report(activities, dir)
  puts "aktiviteter i arket: #{activities.size}"

  without_dimension = activities.reject { |a| a[:dimensions].any? }
  puts "\n-- konsekvensvurdering (kolonne F-I) --"
  puts "   værdier i F: #{activities.filter_map { |a| a[:consequence] }.uniq.sort.join(', ')}"
  puts "   tal i F uden kryds i G/H/I: #{without_dimension.size} " \
       "(#{without_dimension.map { |a| a[:number] }.join(', ')})"
  puts '   NB: importeres ikke i dag - RegisterDTO/RegisterImporter rører ikke consequenceAssessment'

  packages = {}
  Dir.glob(File.join(dir, '*.json')).sort.each do |path|
    packages[normalise(JSON.parse(File.read(path))['name'])] = path
  end
  by_title = activities.to_h { |a| [normalise(a[:name]), a] }

  puts "\n-- mod #{packages.size} pakker i #{dir} --"
  only_sheet = by_title.keys - packages.keys
  only_package = packages.keys - by_title.keys
  puts "   kun i arket (#{only_sheet.size}):"
  only_sheet.each { |t| puts "      + #{t[0, 95]}" }
  puts "   kun i pakkerne (#{only_package.size}) - omdøbt titel kræver en rename i DataBootstrap:"
  only_package.each { |t| puts "      - #{t[0, 95]}  [#{File.basename(packages[t])}]" }

  changed = (by_title.keys & packages.keys).count do |title|
    existing = JSON.parse(File.read(packages[title]))
    (existing['gdprChoices'] || []).sort != by_title[title][:gdpr_choices]
  end
  changed_description = (by_title.keys & packages.keys).count do |title|
    existing = JSON.parse(File.read(packages[title]))
    normalise(existing['description']) != normalise(by_title[title][:description])
  end
  puts "\n   ændret GDPR-hjemmel: #{changed} | ændret beskrivelse: #{changed_description}"
  puts '   (KLE sammenlignes af kle-activity-enricher.rb)'
end

args = ARGV.dup
run_report = !args.delete('--rapport').nil?
out_index = args.index('--ud')
out_dir = out_index ? args.delete_at(out_index + 1) : nil
args.delete('--ud')
workbook = locate_workbook(args.first)

puts "ark: #{File.basename(workbook)}"
activities = read_activities(workbook)
abort 'ingen aktiviteter fundet - er fanen eller rækkenummereringen ændret?' if activities.empty?

report(activities, REGISTERS_DIR) if run_report
write_packages(activities, out_dir) if out_dir
puts "\nintet skrevet - brug --ud MAPPE for at generere pakkerne" unless out_dir
