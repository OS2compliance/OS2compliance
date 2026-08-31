function createDatepicker(inputSelector) {
    return MCDatepicker.create({
        el: inputSelector,
        autoClose: true,
        dateFormat: 'dd/mm-yyyy',
        closeOnBlur: true,
        firstWeekday: 1,
        customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
        customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
        customClearBTN: "Ryd",
        customCancelBTN: "Annuller"
    });
}

function toDkDate(date) {
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    return `${day}/${month}-${date.getFullYear()}`;
}

function parseDkDate(value) {
    const parts = /^(\d{2})\/(\d{2})-(\d{4})$/.exec(value || '');
    return parts ? new Date(Number(parts[3]), Number(parts[2]) - 1, Number(parts[1])) : null;
}

function startOfWeek(date) {
    const result = new Date(date);
    const day = (result.getDay() + 6) % 7;
    result.setDate(result.getDate() - day);
    return result;
}

function addDays(date, days) {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
}

function addWeeks(date, weeks) {
    return addDays(date, weeks * 7);
}

function startOfMonth(date, monthOffset = 0) {
    return new Date(date.getFullYear(), date.getMonth() + monthOffset, 1);
}

function endOfMonth(date, monthOffset = 0) {
    return new Date(date.getFullYear(), date.getMonth() + monthOffset + 1, 0);
}

const PRESETS = [
    { label: 'Sidste uge', range: () => { const s = startOfWeek(addWeeks(new Date(), -1)); return [s, addDays(s, 6)]; } },
    { label: 'Denne uge', range: () => { const s = startOfWeek(new Date()); return [s, addDays(s, 6)]; } },
    { label: 'Næste uge', range: () => { const s = startOfWeek(addWeeks(new Date(), 1)); return [s, addDays(s, 6)]; } },
    { label: 'Sidste måned', range: () => [startOfMonth(new Date(), -1), endOfMonth(new Date(), -1)] },
    { label: 'Denne måned', range: () => [startOfMonth(new Date()), endOfMonth(new Date())] },
    { label: 'Næste måned', range: () => [startOfMonth(new Date(), 1), endOfMonth(new Date(), 1)] },
];

let instanceCounter = 0;

export class DateRangeFilter {
    constructor({ containerEl, getValue, onApply, onClear }) {
        this.containerEl = containerEl;
        this.getValue = getValue;
        this.onApply = onApply;
        this.onClear = onClear;
        this.fromPicker = null;
        this.toPicker = null;
        this.instanceId = instanceCounter++;

        this.render();
        this.applySavedValue();
    }

    render() {
        const fromId = `date-range-filter__from-${this.instanceId}`;
        const toId = `date-range-filter__to-${this.instanceId}`;

        this.containerEl.innerHTML = `
            <div class="date-range-filter">
                <button type="button" class="btn btn-outline-secondary date-range-filter__toggle" data-bs-toggle="dropdown" data-bs-auto-close="outside">
                    <i class="pli-calendar-4 fs-6"></i>
                    <span class="date-range-filter__label">Periode</span>
                </button>
                <span class="date-range-filter__clear" title="Ryd periode" style="display:none;">&times;</span>
                <div class="dropdown-menu p-3 date-range-filter__menu">
                    <div class="d-flex gap-2 mb-3">
                        <div>
                            <label class="form-label mb-1" for="${fromId}">Fra</label>
                            <div class="input-group">
                                <input type="text" id="${fromId}" class="form-control date-range-filter__from" autocomplete="off">
                                <button type="button" class="btn btn-primary date-range-filter__from-btn">
                                    <i class="pli-calendar-4 fs-5"></i>
                                </button>
                            </div>
                        </div>
                        <div>
                            <label class="form-label mb-1" for="${toId}">Til</label>
                            <div class="input-group">
                                <input type="text" id="${toId}" class="form-control date-range-filter__to" autocomplete="off">
                                <button type="button" class="btn btn-primary date-range-filter__to-btn">
                                    <i class="pli-calendar-4 fs-5"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                    <div class="date-range-filter__presets d-grid gap-1 mb-3"></div>
                    <div class="d-flex justify-content-between">
                        <button type="button" class="btn btn-outline-secondary btn-sm date-range-filter__reset">Ryd</button>
                        <button type="button" class="btn btn-primary btn-sm date-range-filter__submit">Anvend</button>
                    </div>
                </div>
            </div>
        `;

        this.toggleButton = this.containerEl.querySelector('.date-range-filter__toggle');
        this.labelEl = this.containerEl.querySelector('.date-range-filter__label');
        this.clearEl = this.containerEl.querySelector('.date-range-filter__clear');
        this.fromInput = this.containerEl.querySelector('.date-range-filter__from');
        this.toInput = this.containerEl.querySelector('.date-range-filter__to');
        this.presetsEl = this.containerEl.querySelector('.date-range-filter__presets');

        this.dropdown = bootstrap.Dropdown.getOrCreateInstance(this.toggleButton, {
            popperConfig: { strategy: 'fixed' },
        });

        this.fromPicker = createDatepicker(`#${fromId}`);
        this.toPicker = createDatepicker(`#${toId}`);
        this.fromInput.addEventListener('click', () => this.fromPicker.open());
        this.toInput.addEventListener('click', () => this.toPicker.open());
        this.containerEl.querySelector('.date-range-filter__from-btn').addEventListener('click', () => this.fromPicker.open());
        this.containerEl.querySelector('.date-range-filter__to-btn').addEventListener('click', () => this.toPicker.open());

        PRESETS.forEach(preset => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'btn btn-outline-secondary btn-sm text-start';
            button.textContent = preset.label;
            button.addEventListener('click', () => {
                const [from, to] = preset.range();
                this.setInputs(from, to);
                this.submit();
            });
            this.presetsEl.appendChild(button);
        });

        this.containerEl.querySelector('.date-range-filter__submit').addEventListener('click', () => this.submit());
        this.containerEl.querySelector('.date-range-filter__reset').addEventListener('click', () => this.clear());
        this.clearEl.addEventListener('click', (event) => {
            event.stopPropagation();
            this.clear();
        });
    }

    setInputs(from, to) {
        this.fromInput.value = from ? toDkDate(from) : '';
        this.toInput.value = to ? toDkDate(to) : '';
        if (from) {
            this.fromPicker.setFullDate(from);
        }
        if (to) {
            this.toPicker.setFullDate(to);
        }
    }

    applySavedValue() {
        const value = this.getValue();
        this.setInputs(parseDkDate(value.from), parseDkDate(value.to));
        this.updateLabel(value.from, value.to);
    }

    submit() {
        const from = this.fromInput.value.trim();
        const to = this.toInput.value.trim();
        this.dropdown.hide();
        this.updateLabel(from, to);
        this.onApply(from, to);
    }

    clear() {
        this.setInputs(null, null);
        this.dropdown.hide();
        this.updateLabel('', '');
        this.onClear();
    }

    updateLabel(from, to) {
        if (!from && !to) {
            this.labelEl.textContent = 'Periode';
            this.clearEl.style.display = 'none';
            return;
        }
        this.labelEl.textContent = `${from || '…'} – ${to || '…'}`;
        this.clearEl.style.display = '';
    }
}
