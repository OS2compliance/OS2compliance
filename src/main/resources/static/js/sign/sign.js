var pdfDoc, pageNum, pageRendering, pageNumPending, scale, zoomRange, canvas, ctx;
function init(url) {
    //Setting initial values
    pdfDoc = null;
    pageNum = 1;
    pageRendering = false;
    pageNumPending = null;
    scale = 1.4;
    zoomRange = 0.1;
    canvas = document.getElementById('pdfCanvas');
    ctx = canvas.getContext('2d');

    //fetching the pdf
    PDFJS.getDocument(url).then(function (pdfDoc_) {
        pdfDoc = pdfDoc_;
        var documentPagesNumber = pdfDoc.numPages;
        document.getElementById('page_count').textContent = '/ ' + documentPagesNumber;

        let page_numInput = document.getElementById('page_num');
        page_numInput.addEventListener('change', function() {
            var pageNumber = this.value;
            if(pageNumber > 0 && pageNumber <= documentPagesNumber) {
                queueRenderPage(pageNumber, scale);
            }
        });

        // Initial/first page rendering
        renderPage(pageNum, scale);
    });

    //add listeners
    document.getElementById('prev').addEventListener('click', onPrevPage);
    document.getElementById('next').addEventListener('click', onNextPage);
    document.getElementById('zoomin').addEventListener('click', onZoomIn);
    document.getElementById('zoomout').addEventListener('click', onZoomOut);
    document.getElementById('zoomfit').addEventListener('click', onZoomFit);

}

//Get page info from document, resize canvas accordingly, and render page.
function renderPage(num, scale) {
    pageRendering = true;
    // Using promise to fetch the page
    pdfDoc.getPage(num).then(function(page) {
        var viewport = page.getViewport(scale);
        canvas.height = viewport.height;
        canvas.width = viewport.width;

        // Render PDF page into canvas context
        var renderContext = {
            canvasContext: ctx,
            viewport: viewport
        };
        var renderTask = page.render(renderContext);

        // Wait for rendering to finish
        renderTask.promise.then(function () {
            pageRendering = false;
            if (pageNumPending !== null) {
                // New page rendering is pending
                renderPage(pageNumPending);
                pageNumPending = null;
            }
        });
    });

    // Update page counters
    document.getElementById('page_num').value = num;
}

/**
 * If another page rendering in progress, waits until the rendering is
 * finised. Otherwise, executes rendering immediately.
 */
function queueRenderPage(num) {
    if (pageRendering) {
        pageNumPending = num;
    } else {
        renderPage(num,scale);
    }
}

/**
 * Displays previous page.
 */
function onPrevPage() {
    if (pageNum <= 1) {
        return;
    }
    pageNum--;
    var scale = pdfDoc.scale;
    queueRenderPage(pageNum, scale);
}

/**
 * Displays next page.
 */
function onNextPage() {
    if (pageNum >= pdfDoc.numPages) {
        return;
    }
    pageNum++;
    var scale = pdfDoc.scale;
    queueRenderPage(pageNum, scale);
}

/**
 * Zoom in page.
 */
function onZoomIn() {
    if (scale >= pdfDoc.scale) {
        return;
    }
    scale += zoomRange;
    var num = pageNum;
    renderPage(num, scale)
}

/**
 * Zoom out page.
 */
function onZoomOut() {
    if (scale >= pdfDoc.scale) {
        return;
    }
    scale -= zoomRange;
    var num = pageNum;
    queueRenderPage(num, scale);
}

/**
 * Zoom fit page.
 */
function onZoomFit() {
    if (scale >= pdfDoc.scale) {
        return;
    }
    scale = 1;
    var num = pageNum;
    queueRenderPage(num, scale);
}

function pageLoaded() {
    init(fetchPdfUrl + documentId);
}
