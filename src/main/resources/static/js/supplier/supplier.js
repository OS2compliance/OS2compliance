
		const defaultClassName = {
			table: 'table table-striped',
			search: "form-control",
			header: "d-flex justify-content-end"
		};

		 let editDialog;

		const updateUrl = (prev, query) => {
			return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
		};

		function deleteClicked(supplierId, name) {
            Swal.fire({
              text: `Er du sikker på du vil slette "${name}"?\nReferencer til og fra leverandøren slettes også.`,
              icon: 'warning',
              showCancelButton: true,
              confirmButtonColor: '#03a9f4',
              cancelButtonColor: '#df5645',
              confirmButtonText: 'Ja',
              cancelButtonText: 'Nej'
            }).then((result) => {
              if (result.isConfirmed) {
                fetch(`${deleteUrl}${supplierId}`, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': token} })
                        .then(() => {
                            window.location.reload();
                        });
              }
            })
		}

		function editClicked(supplierId) {
			fetch(`${formUrl}?id=${supplierId}`)
					.then(response => response.text()
							.then(data => {
								let dialog = document.getElementById('formEditDialog');
								dialog.innerHTML = data;
								editDialog = new bootstrap.Modal(document.getElementById('formEditDialog'));
								editDialog.show();
							}))
					.catch(error => toastService.error(error));
		}

		document.addEventListener("DOMContentLoaded", function(event) {
			fetch(formUrl)
					.then(response => response.text()
							.then(data => document.getElementById('formDialog').innerHTML = data))
					.catch(error => toastService.error(error));

            let gridConfig = {
                className: defaultClassName,
                columns: [
                    {
                        name: "id",
                        hidden: true
                    },
                    {
                        name: "Navn",
                        searchable: {
                            searchKey: 'name'
                        },
                        formatter: (cell, row) => {
                            const url = viewUrl + row.cells[0]['data'];
                            return gridjs.html(`<a href="${url}">${cell}</a>`);
                        },
                        width: '40%'
                    },
                    {
                        name: "Antal løsninger",
                        width: '20%'
                    },
                    {
                        name: "Opdateret",
                        searchable: {
                            searchKey: 'updated'
                        },
                        width: '100px'
                    },
                    {
                        name: "Status",
                        searchable: {
                            searchKey: 'status',
                            fieldId : 'supplierStatusSearchSelector'
                        },
                        width: '100px',
                        formatter: (cell, row) => {
                            var status = cell;
                            if (cell === "Klar") {
                                status = [
                                    '<div class="d-block badge bg-success">' + cell + '</div>'
                                ]
                            } else if (cell === "I gang") {
                                status = [
                                    '<div class="d-block badge bg-info">' + cell + '</div>'
                                ]
                            }
                            return gridjs.html(''.concat(...status), 'div')
                        },
                    },
                    {
                        id: 'handlinger',
                        name: 'Handlinger',
                        sort: 0,
                        width: '90px',
                        formatter: (cell, row) => {
                            const supplierId = row.cells[0]['data'];
                            const name = row.cells[1]['data'].replaceAll("'", "\\'");
                            if(superuser) {
                            return gridjs.html(
                                `<button type="button" class="btn btn-icon btn-outline-light btn-xs me-1" onclick="editClicked('${supplierId}')"><i class="pli-pen-5 fs-5"></i></button>` +
                                `<button type="button" class="btn btn-icon btn-outline-light btn-xs" onclick="deleteClicked('${supplierId}', '${name}')"><i class="pli-trash fs-5"></i></button>`);
                            }
                        }
                    }
                ],
                server:{
                    url: gridSuppliersUrl,
                    method: 'POST',
                    headers: {
                        'X-CSRF-TOKEN': token
                    },
                    then: data => data.suppliers.map(supplier =>
                        [ supplier.id, supplier.name, supplier.solutionCount, supplier.updated, supplier.status ]
                    ),
                    total: data => data.count
                },
                language: {
                    'search': {
                        'placeholder': 'Søg'
                    },
                    'pagination': {
                        'previous': 'Forrige',
                        'next': 'Næste',
                        'showing': 'Viser',
                        'results': 'leverandører',
                        'of': 'af',
                        'to': 'til',
                        'navigate': (page, pages) => `Side ${page} af ${pages}`,
                        'page': (page) => `Side ${page}`
                    }
                }
            };
            const grid = new gridjs.Grid(gridConfig).render( document.getElementById( "suppliersDatatable" ));

            new CustomGridFunctions(grid, gridSuppliersUrl, 'suppliersDatatable')

            gridOptions.init(grid, document.getElementById("gridOptions"));
		});
