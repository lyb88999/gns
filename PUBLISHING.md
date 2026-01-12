# SDK 发布指南

本指南介绍了如何将 GNS SDK 发布到各语言的官方包仓库中。

## 前置准备

- 注册对应仓库的账号：
  - **Python**: [PyPI](https://pypi.org/)
  - **Node.js**: [npm](https://www.npmjs.com/)
  - **Java**: [Maven Central (Sonatype)](https://central.sonatype.org/)
- **Go**: 一个公开的 Git 仓库（如 GitHub/GitLab）。

---

## 1. Python (PyPI)

**目录**: `sdks/python`

1.  **安装构建工具**:
    ```bash
    pip install setuptools wheel twine
    ```

2.  **构建**:
    ```bash
    cd sdks/python
    python3 setup.py sdist bdist_wheel
    ```

3.  **上传到 PyPI**:
    ```bash
    twine upload dist/*
    ```
    *系统会提示您输入 PyPI 的用户名和密码（或 API Token）。*

---

## 2. Node.js (npm)

**目录**: `sdks/nodejs`

1.  **登录 npm**:
    ```bash
    npm login
    ```

2.  **发布**:
    ```bash
    cd sdks/nodejs
    # 确保 package.json 中的 version 是唯一的
    npm publish --access public
    ```

---

## 3. Go (pkg.go.dev)

Go 模块依赖于 **Git Tags**。不需要上传文件到中心仓库，只需将代码推送到公开的 Git 仓库即可。

1.  **推送代码**:
    确保 `sdks/go` 目录已提交并推送到远程仓库。

2.  **打标签 (Tag)**:
    ```bash
    git tag sdks/go/v0.1.0
    git push origin sdks/go/v0.1.0
    ```
    *注意：由于 Go 模块在子目录下，标签命名建议包含路径，或者您也可以将其拆分为独立的仓库。*

3.  **使用**:
    用户可以通过以下命令获取：
    ```bash
    go get github.com/your-username/your-repo/sdks/go
    ```

---

## 4. Java (Maven Central)

**目录**: `sdks/java`

发布到 Maven Central 是最复杂的，通常需要 GPG 签名。

1.  **更新 `pom.xml`**:
    添加 distributionManagement 和 GPG 插件配置（参考 [Sonatype 指南](https://central.sonatype.org/publish/publish-maven/)）。

2.  **部署**:
    ```bash
    cd sdks/java
    mvn clean deploy
    ```
    *(需在本地 `settings.xml` 中配置 OSSRH 凭证)*

### 替代方案：GitHub Packages (简单)

如果是内部使用或想简化流程，可以发布到 GitHub Packages：

1.  在 `pom.xml` 中添加：
    ```xml
    <distributionManagement>
        <repository>
            <id>github</id>
            <name>GitHub Packages</name>
            <url>https://maven.pkg.github.com/YOUR_USERNAME/YOUR_REPO</url>
        </repository>
    </distributionManagement>
    ```
2.  运行 `mvn deploy`。
