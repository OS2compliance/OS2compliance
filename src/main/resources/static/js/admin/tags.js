const tags = new TagService()

function TagService () {
    this.deleteTag = (identifier, name) => {
        console.log('Tag deletion test')
    }
}
