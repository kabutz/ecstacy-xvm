module TestCollections
    {
    @Inject ecstasy.io.Console console;

    void run()
        {
        console.println("Collection tests");

        testLinkedList();
        }

    class Phone
        {
        String desc;
        String number;

        // scenario #1 - all phone numbers in the list, starting with this one
        @LinkedList Phone? next;
        List<Phone> list.get()
            {
            return &next;
            }
        }

    class Person
        {
        String  name;
        Date    dob;

        // scenario #2 - ancestors, starting with this person's parent (assume asexual reproduction,
        // since this is linked list structure, instead of a tree)
        @LinkedList(omitThis=True) Person? parent;
        List<Person> ancestors.get()
            {
            return &parent;
            }

        // scenario #3 - all siblings, including this person
        @LinkedList(prev=prevSibling) Person? nextSibling;
        Person? prevSibling;
        List<Person> siblings.get()
            {
            return &nextSibling;
            }

        // scenario #4 - all children of this person
        @LinkedList(next=nextSibling, prev=prevSibling, omitThis=True) Person? child;
        List<Person> children.get()
            {
            return &child;
            }

        // scenario #5 - all phone numbers of this person
        @LinkedList(Phone.next) Phone? phone; // scenario #5
        List<Phone> phoneNumbers.get()
            {
            return &phone;
            }
        }

    void testLinkedList()
        {
        }
    }