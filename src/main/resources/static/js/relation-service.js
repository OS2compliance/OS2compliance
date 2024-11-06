
let relationService = new RelationService();
function RelationService() {

    this.deleteRelation = (element) => {
        const id = element.dataset.relatableid;
        const relationId = element.dataset.relationid;
        const relationType = element.dataset.relationtype;
        const token = document.getElementsByName("_csrf")[0].getAttribute("content");
        Swal.fire({
            text: `Er du sikker på du vil slette relationen?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#03a9f4',
            cancelButtonColor: '#df5645',
            confirmButtonText: 'Ja',
            cancelButtonText: 'Nej'
        }).then((result) => {
            if (result.isConfirmed) {
                fetch('/relatables/' + id + '/relations/' + relationId + '/' + relationType + '/remove', {
                    method: 'DELETE',
                    headers: {
                        'X-CSRF-TOKEN': token,
                    }
                }).then(() => {
                    window.location.reload();
                });
            }
        });
    }


}
