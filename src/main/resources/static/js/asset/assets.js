    const defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };

    const editTemplate = document.getElementById('editListItemButtonTemplate')
    const deleteTemplate = document.getElementById('deleteListItemButtonTemplate')

    const updateUrl = (prev, query) => {
        return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
    };

    document.addEventListener("DOMContentLoaded", function(event) {
        fetch(formUrl)
            .then(response => {
                if (response.ok) {
                    response.text()
                        .then(data => {
                            document.getElementById('formDialog').innerHTML = data;
                            formLoaded();
                            //initFormValidationForForm('formDialog');
                        })
                }
            })
            .catch(error => {
                toastService.error(error)
            })

        initGrid()

        initGridActionButtons()

    });

    function deleteClicked(assetId, name) {
        Swal.fire({
          text: `Er du sikker på du vil slette "${name}"?\nReferencer til og fra aktivet slettes også.`,
          icon: 'warning',
          showCancelButton: true,
          confirmButtonColor: '#03a9f4',
          cancelButtonColor: '#df5645',
          confirmButtonText: 'Ja',
          cancelButtonText: 'Nej'
        }).then((result) => {
          if (result.isConfirmed) {
            fetch(`${deleteUrl}${assetId}`, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': token} })
                    .then(() => {
                        window.location.reload();
                    });
          }
        })
    }

    async function onEditclicked(assetId) {
        const response = await fetch(`${formUrl}?id=${assetId}`, {
            headers: {
                'X-CSRF-TOKEN': token
            }
        })

        if (!response.ok) {
            toastService.error(response.error)
            console.error("Could not load edit fragment for asset")
        }

        const responseText = await response.text()

        let dialog = document.getElementById('formDialog');
        dialog.innerHTML = responseText;
        editDialog = new bootstrap.Modal(document.getElementById('formDialog'));
        editDialog.show();

    }

    function initGrid() {
        let assetGridConfig = {
            className: defaultClassName,
            columns: [
                {
                    name: "id",
                    hidden: true
                },
                {
                    name:"kitos",
                    hidden: true
                },
                {
                    name: "Navn",
                    searchable: {
                        searchKey: 'name'
                    },
                    formatter: (cell, row) => {
                        const url = viewUrl + row.cells[0]['data'];
                        if(row.cells[1]['data'] == 'true') {
                            return gridjs.html(`<a href="${url}">${cell}</a> <img src="/img/kitos_icon.svg" alt="OS2kitos Logo" width="40" >`);
                        } else {
                            return gridjs.html(`<a href="${url}">${cell}</a>`);
                        }
                    }
                },
                {
                    name: "Leverandør",
                    searchable: {
                        searchKey: 'supplier'
                    }
                },
                {
                    name: "Tredjelandsoverførsel",
                    searchable: {
                        searchKey: 'hasThirdCountryTransfer',
                        fieldId : 'assetThirdCountrySelector'
                    },
                    width: '150px',
                    formatter: (cell, row) => {
                        if (cell) {
                            return 'Ja';
                        } else {
                            return 'Nej';
                        }
                    }
                },
                {
                    name: "Type",
                    width: '100px',
                    searchable: {
                        searchKey: 'assetType'
                    },
                },
                {
                    name: "Systemejer",
                    searchable: {
                        searchKey: 'responsibleUserNames'
                    },
                },
                {
                    name: "Opdateret",
                    searchable: {
                        searchKey: 'updatedAt'
                    },
                    width: '100px'
                },
                {
                    name: "Antal beh.",
                    width: '95px',
                    searchable: {
                        sortKey: 'registers'
                    },
                },
                {
                    name: "Risiko vurdering",
                    searchable: {
                        searchKey: 'assessment',
                        fieldId:'assetRiskSearchSelector'
                    },
                    width: '130px',
                    formatter: (cell, row) => {
                        var assessment = [];
                        if (cell === "Grøn") {
                            assessment = [
                                '<div class="d-block badge bg-green" style="width: 60px">' + cell + '</div>'
                            ]
                        } else if (cell === "Lysgrøn") {
                            assessment = [
                                '<div class="d-block badge bg-green-300" style="width: 60px">' + cell + '</div>'
                            ]
                        } else if (cell === "Gul") {
                            assessment = [
                                '<div class="d-block badge bg-yellow-500" style="width: 60px">' + cell + '</div>'
                            ]
                        } else if (cell === "Orange") {
                            assessment = [
                                '<div class="d-block badge bg-orange" style="width: 60px">' + cell + '</div>'
                            ]
                        } else if (cell === "Rød") {
                            assessment = [
                                '<div class="d-block badge bg-red" style="width: 60px">' + cell + '</div>'
                            ]
                        }
                        return gridjs.html(''.concat(...assessment), 'div')
                    },
                },
                {
                    name: "Status",
                    searchable: {
                        searchKey: 'assetStatus',
                        fieldId : 'assetStatusSearchSelector'
                    },
                    width: '120px',
                    formatter: (cell, row) => {
                        var status = cell;
                        if (cell === "Ikke startet") {
                            status = '<div class="d-block badge bg-warning">' + cell + '</div>'
                        } else if (cell === "I gang") {
                            status = '<div class="d-block badge bg-info">' + cell + '</div>'
                        } else if (cell === "Klar") {
                            status = '<div class="d-block badge bg-success">' + cell + '</div>'
                        }
                        return gridjs.html(status, 'div');
                    },
                },
                {
                    id: 'allowedActions',
                    name: 'Handlinger',
                    sort: 0,
                    width: '90px',
                    formatter: (cell, row) => {
                        const rowId = row.cells[0]['data'];
                        const name = row.cells[2]['data'];
                        return formatAllowedActions(cell, row, rowId, name);
                    }
                }
            ],
            server:{
                url: gridAssetsUrl,
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': token
                },
                then: data => data.content.map(asset =>
                    [ asset.id, asset.kitos, asset.name, asset.supplier, asset.hasThirdCountryTransfer, asset.assetType, asset.responsibleUsers, asset.updatedAt, asset.registers, asset.assessment, asset.assetStatus, asset.allowedActions ],
                ),
                total: data => data.totalCount
            },
            language: {
                'search': {
                    'placeholder': 'Søg'
                },
                'pagination': {
                    'previous': 'Forrige',
                    'next': 'Næste',
                    'showing': 'Viser',
                    'results': 'aktiver',
                    'of': 'af',
                    'to': 'til',
                    'navigate': (page, pages) => `Side ${page} af ${pages}`,
                    'page': (page) => `Side ${page}`
                }
            }
        };
        const grid = new gridjs.Grid(assetGridConfig).render( document.getElementById( "assetsDatatable" ));

        new CustomGridFunctions(grid, gridAssetsUrl, 'assetsDatatable')

        gridOptions.init(grid, document.getElementById("gridOptions"));
    }

    function initGridActionButtons() {
        delegateListItemActions(
            "assetsDatatable",
            (id) => onEditclicked(id),
            (id, name)=> deleteClicked(id, name)
        )
    }