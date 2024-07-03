import json
import requests


def get_customers():
    url = 'https://apisandbox.briostack.io/rest/v1/customers'

    try:

        headers = {'x-api-key': 'your-api-key-here'}
        #  query params to get the last 10 customers added
        params = {'limit': 10, 'filter': 'active=true', 'sort-by': '-dateAcquired'}
        # Perform the GET request
        response = requests.get(url, headers=headers, params=params)

        # Check if the request was successful
        if response.status_code == 200:
            # Parse the JSON response
            customers = response.json()
            # Print each customer
            for customer in customers:
                print(json.dumps(customer, indent=4))

            # extract the customerId of the first customer
            customer_id = customers[0].get('customerId')
            if customer_id:
                # Perform a new GET request to retrieve details of the first customer
                customer_details_url = f"{url}/{customer_id}"
                details_response = requests.get(customer_details_url, headers=headers)

                if details_response.status_code == 200:
                    # Parse and print the details of the first customer
                    customer_details = details_response.json()
                    print("\nDetails of the most recent customer:")
                    print(json.dumps(customer_details, indent=4))
                else:
                    print(
                        f"Failed to get customerId {customer_id}. Status code: {details_response.status_code}")
                    print("Response:", details_response.text)

        else:
            print(f"Failed to retrieve customers. Status code: {response.status_code}")
            print("Response:", response.text)

    except requests.exceptions.RequestException as e:
        # Handle any exceptions that occur during the request
        print(f"An error occurred: {e}")


if __name__ == '__main__':
    get_customers()
