
//let token = document.getElementsByName("_csrf")[0].getAttribute("content");
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
                    const columnIds = ['id', 'name', 'usercount'];
                    const col = columns[0]; // multiColumn false
                    const order = columnIds[col.index];
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
                name: "Navn"
            },
            {
                name: "Antal Brugere",
            },
            {
                id: 'handlinger',
                name: 'Handlinger',
                sort: 0,
                width: '90px',
                formatter: (cell, row) => {
                    if(isSuperuser) {
                        const roleId = row.cells[0]['data'];
                        const name = row.cells[1]['data'].replaceAll("'", "\\'");
                        return gridjs.html(
                            `<button type="button" class="btn btn-icon btn-outline-light btn-xs" onclick="deleteClicked('${roleId}', '${name}')"><i class="pli-trash fs-5"></i></button>`);
                    }
                }
            }
        ],
        server:{
            url: roleUrl,
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': token
            },
            then: data => data.content.map(roleDTO =>
                [ roleDTO.id, roleDTO.name ]
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
                'results': 'Roller',
                'of': 'af',
                'to': 'til',
                'navigate': (page, pages) => `Side ${page} af ${pages}`,
                'page': (page) => `Side ${page}`
            }
        }
    };
    const grid = new gridjs.Grid(gridConfig).render( document.getElementById( "roleDatatable" ));
    searchService.initSearch(grid, gridConfig);
    gridOptions.init(grid, document.getElementById("gridOptions"));
});

function deleteClicked(roleId, name) {
    Swal.fire({
      text: `Er du sikker på du vil slette "${name}"?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#03a9f4',
      cancelButtonColor: '#df5645',
      confirmButtonText: 'Ja',
      cancelButtonText: 'Nej'
    }).then((result) => {
      if (result.isConfirmed) {
        fetch(`${deleteRoleUrl}${roleId}`, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': token} })
                .then(() => {
                    window.location.reload();
                });
      }
    })
}
