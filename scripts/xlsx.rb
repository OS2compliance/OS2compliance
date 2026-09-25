# frozen_string_literal: true

# Minimal xlsx-læser til import-scripterne. Bruger kun stdlib (rexml) plus unzip,
# så der ikke skal installeres gems for at køre en KL-opdatering.
#
#   ark = Xlsx.new("KL-mappede behandlingsaktiviteter ... 1.8.xlsx")
#   ark.rows("Behandlingsaktiviteter") { |nr, celler| celler["D"] }
#
# Tomme celler udelades af hashen, så celler["G"] er nil når der ikke er sat kryds.
require 'rexml/document'

class Xlsx
  MAIN = 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'
  REL = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships'

  def initialize(path)
    raise "arket findes ikke: #{path}" unless File.exist?(path)

    @path = path
    @shared = load_shared_strings
    @sheets = load_sheet_index
  end

  def sheet_names
    @sheets.keys
  end

  # Yielder [rækkenummer, {"A" => "værdi", ...}] for hver række med indhold.
  def rows(sheet_name)
    target = @sheets[sheet_name] or
      raise "fanen #{sheet_name.inspect} findes ikke. Faner i arket: #{sheet_names.join(', ')}"

    doc = REXML::Document.new(entry(target))
    REXML::XPath.each(doc, '//row') do |row|
      cells = {}
      REXML::XPath.each(row, 'c') do |c|
        col = c.attribute('r').to_s[/\A[A-Z]+/]
        value = cell_value(c)
        cells[col] = value if value && !value.empty?
      end
      yield(row.attribute('r').to_s.to_i, cells) unless cells.empty?
    end
  end

  private

  def entry(name)
    data = IO.popen(['unzip', '-p', @path, name], 'rb', &:read)
    raise "kunne ikke læse #{name} fra #{@path} (er unzip installeret?)" unless $?.success?

    data.force_encoding('UTF-8')
  end

  def load_shared_strings
    return [] unless IO.popen(['unzip', '-l', @path, 'xl/sharedStrings.xml'], &:read).include?('sharedStrings')

    doc = REXML::Document.new(entry('xl/sharedStrings.xml'))
    REXML::XPath.match(doc, '/sst/si').map do |si|
      REXML::XPath.match(si, './/t').map(&:text).compact.join
    end
  end

  def load_sheet_index
    rels = {}
    REXML::XPath.each(REXML::Document.new(entry('xl/_rels/workbook.xml.rels')), '//Relationship') do |r|
      rels[r.attribute('Id').to_s] = r.attribute('Target').to_s
    end
    sheets = {}
    REXML::XPath.each(REXML::Document.new(entry('xl/workbook.xml')), '//sheet') do |s|
      target = rels[s.attribute('id', REL).to_s].sub(%r{\A/?(xl/)?}, '')
      sheets[s.attribute('name').to_s] = "xl/#{target}"
    end
    sheets
  end

  def cell_value(cell)
    case cell.attribute('t')&.value
    when 's'
      idx = REXML::XPath.first(cell, 'v')&.text
      idx && @shared[idx.to_i]
    when 'inlineStr'
      REXML::XPath.match(cell, 'is//t').map(&:text).compact.join
    else
      raw = REXML::XPath.first(cell, 'v')&.text
      # 2.0 og 2 skal give samme resultat - konsekvensværdier og KLE-numre
      # skrives forskelligt afhængigt af hvordan cellen er formateret
      raw&.sub(/\A(-?\d+)\.0+\z/, '\1')
    end&.strip
  end
end
