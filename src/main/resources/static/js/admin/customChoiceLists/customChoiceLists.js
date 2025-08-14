class CustomChoiceListService {
    #tableIdentifier = "customChoiceListTable"
    #defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };
    #modal

    constructor() {
        this.initGrid()
        initSaveAsExcelButton();
    }

    initGrid() {
        let gridConfig = {
            className: this.#defaultClassName,
            resizable: true,
            sort: true,
            pagination: true,
            autoWidth: true,
            columns: [
                {
                    id: "id",
                    name: "id",
                    hidden: true
                },
                {
                    id: "name",
                    name: "Titel"
                },
                {
                    id: 'handlinger',
                    name: 'Handlinger',
                    width: "8.3%",
                    sort: 0,
                    formatter: (cell, row) => {
                        if (isSuperuser) {
                            const id = row.cells[0]['data'];
                            return gridjs.html(
                                `<div class="d-flex gap-2">
                                <button type="button" class="btn btn-icon btn-outline-light btn-xs" onclick="customChoiceListService.onEditChoiceList(${id})"><i class="pli-pencil fs-5"></i></button>
                                </div>`);
                        }
                    }
                },
            ],
            data: data,
            language: {
                'search': {
                    'placeholder': 'Søg'
                },
                'pagination': {
                    'previous': 'Forrige',
                    'next': 'Næste',
                    'showing': 'Viser',
                    'results': 'Valglister',
                    'of': 'af',
                    'to': 'til',
                    'navigate': (page, pages) => `Side ${page} af ${pages}`,
                    'page': (page) => `Side ${page}`
                }
            }
        };

        const grid = new gridjs.Grid(gridConfig).render(document.getElementById(this.#tableIdentifier));
    }

    async onEditChoiceList(id) {
        const modalContainerId = 'choiceListEditModalContainer'
        const modalContainer = document.getElementById(modalContainerId)
        const url = `choicelists/${id}/edit`

        if (await networkService.GetFragment(url, modalContainer)) {
            this.#modal = new bootstrap.Modal(modalContainer, {
                backdrop: "static"
            })
            this.#modal.show()
        } else {
            console.error('could not show modal for editing custom choice lists')
        }
    }

    onRemoveChoiceValueField(event) {
        const target = event.target.closest(".choice-value-field-container")
        this.#removeChoiceValueField(target)
    }

    #removeChoiceValueField(fieldElement) {
        if (fieldElement) {
            fieldElement.remove()
        } else {
            console.error('could not remove choice value elements', fieldElement)
        }

    }

    onGenerateChoiceValueField(event) {
        const button = event.target
        const newValueField = this.#generateChoiceFieldElements()
        button.parentElement.insertBefore(newValueField, button)
        newValueField.querySelector('.choice-value-field').focus()

    }

    #generateChoiceFieldElements() {
        const newValueFieldContainer = document.createElement('div')
        newValueFieldContainer.classList.add("form-group", "d-flex", "gap-2", "align-items-center")

        const inputField = document.createElement('input')
        inputField.classList.add("choice-value-field", "form-control")
        inputField.type = "text"
        newValueFieldContainer.appendChild(inputField)


        const removebutton = document.createElement('button')
        removebutton.classList.add("btn", "btn-icon", "btn-outline-light", "btn-s", "align-middle", "bg-danger")
        removebutton.type = "button"
        removebutton.addEventListener('click', (e) => this.#removeChoiceValueField(newValueFieldContainer))
        newValueFieldContainer.appendChild(removebutton)

        const buttonIcon = document.createElement('i')
        buttonIcon.classList.add("pli-remove", "fs-5")
        removebutton.appendChild(buttonIcon)

        return newValueFieldContainer

    }

    async onChoiceListValuesSubmission(listId) {
        // find all input fields, map to id-value object and filter empty values
        const values = Array.from(document.querySelectorAll('.choice-value-field'))
            .map(input => {
                const id = input.id.split('-')[0]
                return {
                    id: id ? id : null,
                    value: input.value
                }
            })
            .filter(choiceValue => choiceValue.value)

        // send objects to rest endpoint
        const url = `${restUrl}/${listId}/update`

        const resultOK = networkService.Put(url, values)
        if (!resultOK) {
            toastService.error("De ændrede værdier blev ikke accepteret af serveren, og er ikke gemt")
        }

        // remove focus from all elements before closing modal (created warning in the console - annoying)
        const modalElement = document.querySelector('#choiceListEditModalContainer');
        if (modalElement) {
            const focusedElement = modalElement.querySelector(':focus');
            if (focusedElement) {
                focusedElement.blur();
            }
        }

        // dismiss modal
        this.#modal.hide()
    }
}

function initSaveAsExcelButton() {
    const saveAsExcelButton = document.getElementById("saveAsExcelButton");
    saveAsExcelButton.addEventListener("click", () => exportHtmlTableToExcel('customChoiceListTable', 'Valglister'))
}