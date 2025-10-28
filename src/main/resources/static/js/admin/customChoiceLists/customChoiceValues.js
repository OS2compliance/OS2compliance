class CustomChoiceValuesService {
    #tableIdentifier = "choiceValueTable"
    #defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };
    #modal

    constructor() {
        this.initGrid()
        this.initButtons();
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
                    id: "caption",
                    name: "Titel"
                },
                {
                    id: "description",
                    name: "Beskrivelse"
                },
                {
                    id: "editable",
                    name: "Kan redigeres?",
                    hidden: true
                },
                {
                    id: 'handlinger',
                    name: 'Handlinger',
                    width: "8.3%",
                    sort: 0,
                    formatter: (cell, row) => {
                        const editable = row.cells[3]['data'];
                        if (editable) {
                            const id = row.cells[0]['data'];
                            const caption = row.cells[1]['data'];
                            const description = row.cells[2]['data'];

                            return gridjs.html(
                                `<div class="d-flex gap-2">
                                <button type="button" class="btn btn-icon btn-outline-light btn-xs"onclick="customChoiceValuesService.onEditChoiceList(${id}, '${caption}', '${description}')"><i class="pli-pencil fs-5"></i></button>
                                <button type="button" class="btn btn-icon btn-outline-light btn-xs" onclick="customChoiceValuesService.onDeleteChoiceValue(${id})"><i class="pli-trash fs-5"></i></button>
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

    onEditChoiceList(id, caption, description) {
        const modalContainerId = 'createOrEditChoiceValueModal';
        const modalContainer = document.getElementById(modalContainerId);
        const form = document.getElementById('choiceValueForm');
        const modalTitle = document.getElementById('choiceValueModalTitle');

        // Set form action for edit
        form.action = `/rest/choicelists/custom/${id}/edit`;
        modalTitle.textContent = 'Rediger';

        // Set form fields
        document.getElementById('name').value = caption;
        document.getElementById('description').value = description === "null" ? '' : description;

        const modal = new bootstrap.Modal(modalContainer);
        modal.show();
    }

    async onCreateChoiceList() {
        const modalContainerId = 'createOrEditChoiceValueModal';
        const modalContainer = document.getElementById(modalContainerId);
        const form = document.getElementById('choiceValueForm');
        const modalTitle = document.getElementById('choiceValueModalTitle');

        // Set form action for create
        form.action = `/admin/choicelists/custom/${choiceListId}/create`;
        modalTitle.textContent = 'Opret ny';

        // Clear form fields
        document.getElementById('name').value = '';
        document.getElementById('description').value = '';

        form.onsubmit = async (e) => {
            e.preventDefault();

            const caption = document.getElementById('name').value;
            const description = document.getElementById('description').value;

            const data = {
                caption: caption,
                description: description
            };

            try {
                await networkService.Post(form.action, data);
            } catch (error) {
                // TODO: It errors for some reason? Talk to julius about this one maybe
            }

            // Always redirect after POST
            window.location.href = `/admin/choicelists/choice/view/${choiceListId}`;
        };

        const modal = new bootstrap.Modal(modalContainer);
        modal.show();
    }

    initButtons() {
        const createButton = document.getElementById('createChoiceValueButton');
        createButton.addEventListener('click', () => this.onCreateChoiceList());
    }

    onDeleteChoiceValue(id) {
        Swal.fire({
            title: 'Bekræft fjernelse',
            text: `Er du sikker på at du vil fjerne denne værdi?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Ja, fjern det!',
            cancelButtonText: 'Annuller'
        }).then((result) => {
            if (result.isConfirmed) {
                fetch(`/choicelists/${id}/delete/${choiceListId}`, {
                    method: 'POST'
                }).then(response => {
                    if (response.ok) {
                        toastService.info("Værdi slettet");
                    }
                    else {
                        toastService.error("Der opstod en teknisk fejl");
                    }
                });
            }
        });
    }
}

