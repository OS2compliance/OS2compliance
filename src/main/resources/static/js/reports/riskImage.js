export default class RiskImageRapportDialog {
    #revealerClass = 'reveals-hidden-on-change'
    #revealTargetClass = 'revealed-on-change'
    fromDate = ''
    toDate = '';

    init() {
        this.#initHiddenCheckboxes()
        this.#initDatePickers()
        // this.#initDownloadButton()
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
            //minDate: new Date(),
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
            //minDate: new Date(),
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

    // #initDownloadButton() {
    //     const downloadButton = document.getElementById('riskImageDownloadButton');
    //     downloadButton.addEventListener('click', (e) => {
    //         const url =`/report/riskimage`
    //         const form = document.getElementById('riskImageReportForm');
    //         form.requestSubmit()
    //
    //         // this.#downloadFile(url)
    //     })
    // }

    async #downloadFile(url) {
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const blob = await response.blob();

        // Extract filename from Content-Disposition header
        let filename = 'Risikobillede'; // fallback
        const contentDisposition = response.headers.get('Content-Disposition');
        if (contentDisposition) {
            const filenameMatch = RegExp(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/).exec(contentDisposition);
            if (filenameMatch?.[1]) {
                filename = filenameMatch[1].replace(/['"]/g, '');
            }
        }

        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = filename;
        a.click();

        URL.revokeObjectURL(a.href);
        a.remove();
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