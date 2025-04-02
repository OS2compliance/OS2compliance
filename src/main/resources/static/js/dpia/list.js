const defaultClassName = {
    table: 'table table-striped',
    search: "form-control",
    header: "d-flex justify-content-end"
};

const updateUrl = (prev, query) => {
    return prev + (prev.indexOf('?') >= 0 ? '&' : '?') + new URLSearchParams(query).toString();
};

document.addEventListener("DOMContentLoaded", function(event) {
    const defaultClassName = {
        table: 'table table-striped',
        search: "form-control",
        header: "d-flex justify-content-end"
    };

    let gridConfig = {
        className: defaultClassName,
        search: {
            keyword: searchService.getSavedSearch(),
            server: {
                url: (prev, keyword) => updateUrl(prev, `search=${keyword}`)
            },
            debounceTimeout: 1000
        },
        pagination: {
            limit: 50,
            server: {
                url: (prev, page, size) => updateUrl(prev, `size=${size}&page=${page}`)
            }
        },
        sort: {
            enabled: true,
            multiColumn: false,
            server: {
                url: (prev, columns) => {
                    if (!columns.length) return prev;
                    const columnIds = columnProperties
                    const col = columns[0]; // always sort by the fiŕst in array
                    const order = columnIds[col.index || 1];
                    return updateUrl(prev, 'dir=' + (col.direction === 1 ? 'asc' : 'desc') + ( order ? '&order=' + order : ''));
                }
            }
        },
        columns: [
            {
                name: "id",
                hidden: true
            },
            {
                name: "Aktiv",
                formatter: (cell, row) => {
                    const url = baseUrl + "/" + row.cells[0]['data'];
                    const isExternal = row.cells[4]['data'];
                    if (isExternal) {
                        return gridjs.html(`<a href="${url}" target="_blank">${cell} (Ekstern)</a>`);
                    } else {
                        return gridjs.html(`<a href="${url}">${cell}</a>`);
                    }
                }
            },
            {
                name: "Opdateret",
                width: '10%'
            },
            {
                name: "Opgaver",
                width: '10%'
            },
            {
                id: 'handlinger',
                name: 'Handlinger',
                sort: 0,
                width: '10%',
                formatter: (cell, row) => {
                    const dpiaId = row.cells[0]['data'];
                    const name = row.cells[1]['data'].replaceAll("'", "\\'");
                    const isExternal = row.cells[4]['data'];
                    let buttonHTML = ""
                    if(superuser) {
                        buttonHTML = buttonHTML + `<button type="button" class="btn btn-icon btn-outline-light btn-xs ms-1" onclick="deleteClicked('${dpiaId}', '${name.replaceAll('\"', '')}')"><i class="pli-trash fs-5"></i></button>`
                    }
                    if (superuser && isExternal) {
                        buttonHTML = buttonHTML + `<button type="button" class="btn btn-icon btn-outline-light btn-xs ms-1" onclick="createExternalDPIAService.editExternalClicked('${dpiaId}')"><i class="pli-pencil fs-5"></i></button>`
                    }
                    return gridjs.html(buttonHTML);
                }
            }
        ],
        server:{
            url: listDataUrl,
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            then: data => data.content.map(obj => {
                    const result = []
                    for (const property of columnProperties) {
                        result.push(obj[property])
                    }
                    return result;
                }
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
                'results': 'Konsekvensanalyser',
                'of': 'af',
                'to': 'til',
                'navigate': (page, pages) => `Side ${page} af ${pages}`,
                'page': (page) => `Side ${page}`
            }
        }
    };
    const grid = new gridjs.Grid(gridConfig).render( document.getElementById( "dpiaDatatable" ));
    searchService.initSearch(grid, gridConfig);
    gridOptions.init(grid, document.getElementById("gridOptions"));
});

function deleteClicked(dpiaId, name) {
    Swal.fire({
      text: `Er du sikker på du vil slette "${name}"?\nReferencer afhængige af konsekvensvurderingen slettes også.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#03a9f4',
      cancelButtonColor: '#df5645',
      confirmButtonText: 'Ja',
      cancelButtonText: 'Nej'
    }).then((result) => {
      if (result.isConfirmed) {
        fetch(`${deleteUrl}/${dpiaId}`, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': token} })
                .then(() => {
                    window.location.reload();
                });
      }
    })
}
