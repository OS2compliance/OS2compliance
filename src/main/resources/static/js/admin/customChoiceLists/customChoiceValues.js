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
        this.initGridActions();
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
                                    <button type="button" class="btn btn-icon btn-outline-light btn-xs editBtn" data-id="${id}" data-caption="${caption}" data-description="${description}"><i class="pli-pencil fs-5"></i></button>
                                    <button type="button" class="btn btn-icon btn-outline-light btn-xs deleteBtn" data-id="${id}"><i class="pli-trash fs-5"></i></button>
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
                    'results': 'Valglist værdier',
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

        form.action = `/rest/choicelists/custom/${choiceListId}/${id}/edit`;
        modalTitle.textContent = 'Rediger';

        document.getElementById('name').value = caption;
        document.getElementById('description').value = description === "null" ? '' : description;

        form.onsubmit = async (e) => {
            e.preventDefault();

            const updatedCaption = document.getElementById('name').value;
            const updatedDescription = document.getElementById('description').value;

            const data = {
                caption: updatedCaption,
                description: updatedDescription
            };

            const response = await networkService.Post(form.action, data);
            if (response.success) {
                window.location.href = `/admin/choicelists/choice/view/${choiceListId}`;
            } else {
                toastService.error("Der opstod en teknist fejl: ", response.error);
            }
        };

        const modal = new bootstrap.Modal(modalContainer);
        modal.show();
    }

    async onCreateChoiceList() {
        const modalContainerId = 'createOrEditChoiceValueModal';
        const modalContainer = document.getElementById(modalContainerId);
        const form = document.getElementById('choiceValueForm');
        const modalTitle = document.getElementById('choiceValueModalTitle');

        form.action = `/rest/choicelists/custom/${choiceListId}/create`;
        modalTitle.textContent = 'Opret ny';

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

            const response = await networkService.Post(form.action, data);
            if (response.success) {
                window.location.href = `/admin/choicelists/choice/view/${choiceListId}`;
            } else {
                toastService.error("Der opstod en teknist fejl: ", response.error);
            }
        };

        const modal = new bootstrap.Modal(modalContainer);
        modal.show();
    }

    initButtons() {
        const createButton = document.getElementById('createChoiceValueButton');
        createButton.addEventListener('click', () => this.onCreateChoiceList());
    }

    initGridActions() {
        delegateListItemActions('choiceValueTable',
            (id, elem) => customChoiceValuesService.onEditChoiceList(elem.dataset.id, elem.dataset.caption, elem.dataset.description),
            (id, name, elem) => customChoiceValuesService.onDeleteChoiceValue(elem.dataset.id),
        )
    }

    async onDeleteChoiceValue(id) {
        const result = await Swal.fire({
            title: 'Bekræft fjernelse',
            text: `Er du sikker på at du vil fjerne denne værdi?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Ja, fjern det!',
            cancelButtonText: 'Annuller'
        });

        if (result.isConfirmed) {
            const response = await fetch(`/rest/choicelists/custom/${choiceListId}/${id}/delete`, {
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': networkService.XCSRFToken || token,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    toastService.info("Værdi slettet");
                    window.location.reload();
                } else {
                    toastService.error("Kunne ikke slette værdi: ", data.error);
                }
            }
        }
    }
}

