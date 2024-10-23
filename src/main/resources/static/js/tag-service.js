
let tagService = new TagService();
function TagService() {

    this.deleteTag = (element) => {
        const id = element.dataset.relatableid;
        const tagId = element.dataset.tagid;
        const token = document.getElementsByName("_csrf")[0].getAttribute("content");

        fetch('/relatables/' + id + '/tags/' + tagId + '/remove', {
            method: 'DELETE',
            headers: {
                'X-CSRF-TOKEN': token,
            }
        }).then(() => {
            window.location.reload();
        });
    }


}
