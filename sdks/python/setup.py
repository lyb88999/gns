from setuptools import setup, find_packages

setup(
    name="gns-sdk",
    version="0.1.0",
    description="General Notification System Client SDK",
    packages=find_packages(),
    install_requires=[
        "requests>=2.25.0",
    ],
    python_requires=">=3.7",
)
