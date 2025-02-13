from flask import Flask, request, jsonify, abort
import subprocess
import os

app = Flask(__name__)

# Set the path to the directory containing your docker-compose.yml
DOCKER_COMPOSE_DIR = '/home/ec2-user/'
TOKEN = '49cf14928048efa5569abb0b6330250c'

@app.route('/deploy', methods=['POST'])
def deploy():
    # Check for a valid authorization token
    if request.headers.get('Authorization') != f'Bearer {TOKEN}':
        abort(403)  # Forbidden
    try:
        # Make sure the Docker Compose directory exists
        if not os.path.isdir(DOCKER_COMPOSE_DIR):
            return jsonify({'error': 'Docker Compose directory not found'}), 404

        # Navigate to the directory where your docker-compose.yml file is located
        os.chdir(DOCKER_COMPOSE_DIR)

        # Run the 'docker-compose up' command
        subprocess.run(['docker-compose', 'up', '-d'], check=True)

        return jsonify({'message': 'Deployment successful'}), 200
    except subprocess.CalledProcessError as e:
        return jsonify({'error': 'Docker Compose failed', 'details': str(e)}), 500

if __name__ == '__main__':
    # Start the Flask app on port 5000
    app.run(host='0.0.0.0', port=5000)