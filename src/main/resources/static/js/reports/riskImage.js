export default class RiskImageRapportDialog {
    #revealerClass = 'reveals-hidden-on-change'
    #revealTargetClass = 'revealed-on-change'
    fromDate = ''
    toDate = '';

    init() {
        this.#initHiddenCheckboxes()
        this.#initDatePickers()
    }

    #initHiddenCheckboxes() {
        const revealers = document.querySelectorAll(`input.${this.#revealerClass}`)
        for (const revealer of revealers) {
            revealer.addEventListener('change', (e) => {
                const revealTarget = revealer.parentElement.querySelector(`.${this.#revealTargetClass}`)
                if (revealTarget && revealer.checked === true) {
                    revealTarget.hidden = false
                } else if (revealTarget) {
                    revealTarget.hidden = true
                }
            })
        }
    }

    #initDatePickers() {
        const dateNow = new Date();
        const dateMonthAgo = new Date(new Date().setMonth(dateNow.getMonth() - 1));

        const fromInputField = document.getElementById('riskImageReportFromDate')
        const toInputField = document.getElementById('riskImageReportToDate')

        fromInputField.value = this.#uiFormatDate(dateMonthAgo)
        this.fromDate = javaFormatDate(dateMonthAgo)
        toInputField.value =  this.#uiFormatDate(dateNow);
        this.toDate = javaFormatDate(dateNow)

        const fromPicker = MCDatepicker.create({
            el: '#riskImageReportFromDate',
            autoClose: true,
            dateFormat: 'dd/mm-yyyy',
            closeOnBlur: true,
            firstWeekday: 1,
            customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
            customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
            customClearBTN: "Ryd",
            customCancelBTN: "Annuller",
            selectedDate: dateMonthAgo,
            maxDate: dateNow,
        });

        const toPicker = MCDatepicker.create({
            el: '#riskImageReportToDate',
            autoClose: true,
            dateFormat: 'dd/mm-yyyy',
            closeOnBlur: true,
            firstWeekday: 1,
            customWeekDays: ["sø", "ma", "ti", "on", "to", "fr", "lø"],
            customMonths: ["Januar", "Februar", "Marts", "April", "Maj", "Juni", "Juli", "August", "September", "Oktober", "November", "December"],
            customClearBTN: "Ryd",
            customCancelBTN: "Annuller",
            selectedDate: dateNow,
            maxDate: dateNow,
        });

        document.getElementById('riskImageReportFromDateBtn').addEventListener("click", () => {
            fromPicker.open();
        });

        document.getElementById('riskImageReportToDateBtn').addEventListener("click", () => {
            toPicker.open();
        });

        fromPicker.onSelect((date, formatedDate) => {
            this.fromDate = javaFormatDate(date);
        });
        toPicker.onSelect((date, formatedDate) => {
            this.toDate = javaFormatDate(date);
        });
    }

    #uiFormatDate(date) {
        if (date === null || date === '') {
            return '';
        }
        let dd = "" + date.getDate();
        if (dd.length === 1) {
            dd = "0" + dd;
        }
        let mm = "" + (date.getMonth()+1);
        if (mm.length === 1) {
            mm = "0" + mm;
        }
        let yyyy = date.getFullYear();
        return `${dd}/${mm}-${yyyy}`;
    }

}