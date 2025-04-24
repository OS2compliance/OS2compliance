        let grid = null;

		const defaultClassName = {
			table: 'table table-striped',
			search: "form-control",
			header: "d-flex justify-content-end"
		};

		const updateUrl = (prev, query) => {
			return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
		};

		document.addEventListener("DOMContentLoaded", function(event) {
            let gridConfig = {
                className: defaultClassName,
                columns: [
                    {
                        name: "id",
                        hidden: true
                    },
                    {
                        name: "Titel",
                        searchable : {
                            searchKey : 'name'
                        },
                        formatter: (cell, row) => {
                            const url = viewUrl + row.cells[0]['data'];
                            return gridjs.html(`<a href="${url}">${cell}</a>`);
                        }
                    },
                    {
                        name: "Afdeling",
                        searchable : {
                            searchKey : 'responsibleOUNames'
                        },
                    },
                    {
                        name: "Forvaltning",
                        searchable : {
                            searchKey : 'departmentNames'
                        },
                    },
                    {
                        name: "Kontakt",
                        searchable : {
                            searchKey : 'responsibleUserNames'
                        },
                    },
                    {
                        name: "Opdateret",
                        searchable : {
                            searchKey : 'updatedAt'
                        },
                        width: "100px"
                    },
                    {
                        name: "Konsekvens vurdering",
                        searchable : {
                            searchKey : 'consequence',
                            fieldId : 'registerConsequenceSearchSelector'
                        },
                        width: "110px",
                        formatter: (cell, row) => {
                            let assessment = cell;
                            if (cell === "Grøn") {
                                assessment = '<div class="badge bg-green align-top" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Lysgrøn") {
                                assessment = '<div class="badge bg-green-300 align-top" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Gul") {
                                assessment = '<div class="badge bg-yellow-500 align-top" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Orange") {
                                assessment = '<div class="badge bg-orange align-top" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Rød") {
                                assessment = '<div class="badge bg-red align-top" style="width: 60px">' + cell + '</div>';
                            }
                            return gridjs.html(assessment, 'div');
                        }
                    },
                    {
                        name: "Risiko vurdering",
                        searchable : {
                            searchKey : 'risk',
                            fieldId : 'registerRiskSearchSelector'
                        },
                        width: "150px",
                        formatter: (cell, row) => {
                            let assessment = '';
                            if (cell === "Grøn") {
                                assessment = '<div class="d-block badge bg-green" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Lysgrøn") {
                                assessment = '<div class="d-block badge bg-green-300" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Gul") {
                                assessment = '<div class="d-block badge bg-yellow-500" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Orange") {
                                assessment = '<div class="d-block badge bg-orange" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Rød") {
                                assessment = '<div class="d-block badge bg-red" style="width: 60px">' + cell + '</div>';
                            }
                            return gridjs.html(assessment, 'div');
                        },
                    },
                    {
                        name: "Risiko aktiver",
                        searchable : {
                            searchKey : 'assetAssessment',
                            fieldId : 'registerAssetSearchSelector'
                        },
                        width: "150px",
                        formatter: (cell, row) => {
                            let assessment = '';
                            if (cell === "Grøn") {
                                assessment = '<div class="d-block badge bg-green" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Lysgrøn") {
                                assessment = '<div class="d-block badge bg-green-300" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Gul") {
                                assessment = '<div class="d-block badge bg-yellow-500" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Orange") {
                                assessment = '<div class="d-block badge bg-orange" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Rød") {
                                assessment = '<div class="d-block badge bg-red" style="width: 60px">' + cell + '</div>';
                            }
                            return gridjs.html(assessment, 'div');
                        },
                    },
                    {
                        name: "Status",
                        searchable : {
                            searchKey : 'status',
                            fieldId : 'registerStatusSearchSelector'
                        },
                        width: '150px',
                        formatter: (cell, row) => {
                            let status = cell;
                            if (cell === "Klar") {
                                status = '<div class="d-block badge bg-success" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "I gang") {
                                status = '<div class="d-block badge bg-info" style="width: 60px">' + cell + '</div>';
                            } else if (cell === "Ikke startet") {
                                status = '<div class="d-block badge bg-danger" style="width: 60px">' + cell + '</div>';
                            }
                            return gridjs.html(status, 'div');
                        },
                    },
                    {
                        name: "Aktiver",
                        width: "100px"
                    },
                    {
                        id: 'handlinger',
                        name: 'Handlinger',
                        sort: 0,
                        width: '90px',
                        formatter: (cell, row) => {
                            const registerId = row.cells[0]['data'];
                            const name = row.cells[1]['data'].replaceAll("'", "\\'");
                            if(superuser) {
                                return gridjs.html(
                                    `<button type="button" class="btn btn-icon btn-outline-light btn-xs" onclick="editRegisterService.show('${registerId}')"><i class="pli-pencil fs-5"></i></button>`
                                    + `<button type="button" class="btn btn-icon btn-outline-light btn-xs" onclick="deleteClicked('${registerId}', '${name}')"><i class="pli-trash fs-5"></i></button>`);
                            }
                        }
                    }
                ],
                server:{
                    url: gridRegistersUrl,
                    method: 'POST',
                    headers: {
                        'X-CSRF-TOKEN': token
                    },
                    then: data => data.content.map(register =>
                        [ register.id, register.name, register.responsibleOUs, register.departments, register.responsibleUsers, register.updatedAt, register.consequence, register.risk, register.assetAssessment, register.status, register.assetCount ]
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
                        'results': 'Fortegnelser',
                        'of': 'af',
                        'to': 'til',
                        'navigate': (page, pages) => `Side ${page} af ${pages}`,
                        'page': (page) => `Side ${page}`
                    }
                }
            };
            grid = new gridjs.Grid(gridConfig).render( document.getElementById( "registersDatatable" ));
//            searchService.initSearch(grid, gridConfig);

            new CustomGridFunctions(grid ,gridRegistersUrl, 'registersDatatable')

            gridOptions.init(grid, document.getElementById("gridOptions"));
		});

		function deleteClicked(registerId, name) {
			Swal.fire({
			  text: `Er du sikker på du vil slette "${name}"?\nReferencer til og fra REGISTER slettes også.`,
			  icon: 'warning',
			  showCancelButton: true,
			  confirmButtonColor: '#03a9f4',
			  cancelButtonColor: '#df5645',
			  confirmButtonText: 'Ja',
			  cancelButtonText: 'Nej'
			}).then((result) => {
			  if (result.isConfirmed) {
				fetch(`${deleteUrl}${registerId}`, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': token} })
						.then(() => {
							window.location.reload();
						});
			  }
			})
		}
