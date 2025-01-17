module FizzBuzz
    {
    void run()
        {
        @Inject Console console;
        for (Int x : 1..100)
            {
            console.print(switch (x % 3, x % 5)
                {
                case (0, 0): "FizzBuzz";
                case (0, _): "Fizz";
                case (_, 0): "Buzz";
                case (_, _): x.toString();
                });
            }
        }
    }
