import ecstasy.fs.Directory;
import ecstasy.fs.File;
import ecstasy.fs.FileNode;

/**
 * A web service that serves static content.
 * The content can be a single file or a directory of files.
 */
const StaticContent
    {
    construct(FileNode fileNode, MediaType mediaType)
        {
        assert:arg fileNode.exists && fileNode.readable;
        this.fileNode  = fileNode;
        this.mediaType = mediaType;
        }

    private FileNode fileNode;

    private MediaType mediaType;

    @Get()
    conditional HttpResponse get()
        {
        FileNode f = fileNode;
        if (f.is(File))
            {
            HttpResponse response = new HttpResponse();
            response.body = f.contents;
            response.headers.setContentType(mediaType);
            return True, response;
            }
        return False;
        }

    @Get("/{name}")
    conditional HttpResponse getResource(@PathParam("name") String name)
        {
        FileNode dir = fileNode;
        if (dir.is(Directory))
            {
            if (File file := dir.findFile(name))
                {
                HttpResponse response = new HttpResponse();
                response.body = file.contents;
                response.headers.setContentType(mediaType);
                return True, response;
                }
            }
        return False;
        }
    }