package com.apihub.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "api_endpoints")
public class ApiEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_id")
    private ApiDefinition api;

    @Column(nullable = false, length = 120)
    private String name;

    /** e.g. /api/weather/{city} */
    @Column(nullable = false, length = 300)
    private String path;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod = "GET";

    @Column(length = 600)
    private String description;

    /**
     * Parameter metadata as a JSON string.
     * WHY a JSON column instead of a parameters table: the shape varies wildly per
     * endpoint and is only ever read as a whole to render the Explorer form.
     * Normalising it would add joins with zero query benefit.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String parameters;

    @Lob
    @Column(name = "response_example", columnDefinition = "TEXT")
    private String responseExample;

    @Column(nullable = false)
    private boolean active = true;

    protected ApiEndpoint() { }

    public ApiEndpoint(String name, String path, String httpMethod, String description,
                       String parameters, String responseExample) {
        this.name = name;
        this.path = path;
        this.httpMethod = httpMethod;
        this.description = description;
        this.parameters = parameters;
        this.responseExample = responseExample;
    }

    public Long getId() { return id; }
    public ApiDefinition getApi() { return api; }
    public void setApi(ApiDefinition api) { this.api = api; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
    public String getResponseExample() { return responseExample; }
    public void setResponseExample(String responseExample) { this.responseExample = responseExample; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
