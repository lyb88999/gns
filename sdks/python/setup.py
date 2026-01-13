from setuptools import setup, find_packages

setup(
    name="gns-sdk-lyb",
    version="0.1.0",
    description="General Notification System Client SDK",
    long_description=open("README.md", encoding="utf-8").read(),
    long_description_content_type="text/markdown",
    author="Your Name",
    author_email="your.email@example.com",
    url="https://github.com/yourusername/gns",
    packages=find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
    install_requires=[
        "requests>=2.25.0",
    ],
    python_requires=">=3.7",
)
