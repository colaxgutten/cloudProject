package com.me.cloudProject.cloudProject;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.translate.Language;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import net.java.html.json.Function;
import net.java.html.json.Model;
import net.java.html.json.Property;
import net.java.html.json.ModelOperation;
import com.me.cloudProject.cloudProject.js.PlatformServices;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

@Model(className = "Data", targetId="", instance=true, properties = {
    @Property(name = "active", type = String.class),
    @Property(name = "titles", type = String.class, array = true),
    @Property(name = "selectedTitle", type = String.class),
    @Property(name = "textcontent", type = String.class),
    @Property(name = "translatedTitle", type = String.class),
    @Property(name = "translatedContent", type = String.class),
    @Property(name = "username", type = String.class),
    @Property(name = "password", type = String.class),
    @Property(name = "selected", type = String.class),
    @Property(name = "selectedLanguage", type = String.class),
    @Property(name = "languages", type = String.class, array = true)
})
final class DataModel {
    private PlatformServices services;
    private final String PAGE_LOGIN="login";
    private final String PAGE_REGISTER="register";
    private final String PAGE_MAINPAGE="mainpage";
    private final String KEY_PATH="src\\main\\java\\com\\me\\cloudProject\\cloudProject\\key\\TestProject-ddb9dc1083b8.json";
    private final String PROJECT_ID="testproject-181612";
    private final String TEXTENTITY_KIND="text";
    private final String TEXTENTITY_TITLE="title";
    private final String TEXTENTITY_CONTENT="content";
    private final String USERENTITY_KIND="User";
    private final String USERENTITY_USERNAME="username";
    private final String USERENTITY_PASSWORD="password";
    private Datastore datastore;
    private Translate translate;
    

    @ModelOperation
    void initServices(Data model, PlatformServices services) {
        this.services = services;
    }
    
    @Function
    public void login() throws Exception{
        DatastoreOptions options = DatastoreOptions.newBuilder()
                .setProjectId(PROJECT_ID)
                .setCredentials(GoogleCredentials.fromStream(
                        new FileInputStream(KEY_PATH))).build(); 
        datastore = options.getService();
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(USERENTITY_KIND)
                .setFilter(PropertyFilter.eq(USERENTITY_USERNAME, ui.getUsername()))
                .setFilter(PropertyFilter.eq(USERENTITY_PASSWORD, ui.getPassword())).build();
        QueryResults<Entity> result = datastore.run(query);
        //weak check but should only be one result if login matches
        if (result.hasNext()){
            Entity e = result.next();
            loadTitles(datastore);
            loadLanguages();
        ui.setActive(PAGE_MAINPAGE);
        }
    }
    
    @Function
    public void registerView() {
        ui.setActive(PAGE_REGISTER);
    }
    
    private void loadLanguages() throws Exception{
        if (translate==null){
            authenticateTranslate();
        }
        ArrayList<String> languages = new ArrayList();
        for (Language l : translate.listSupportedLanguages()){
            String s = "";
            s+=l.getCode()+" ("+l.getName()+")";
            languages.add(s);
        }
        ui.getLanguages().clear();
        ui.getLanguages().addAll(languages);
    }
    
    private void authenticateTranslate() throws Exception{
        TranslateOptions options = TranslateOptions.newBuilder().setProjectId(PROJECT_ID).setCredentials(GoogleCredentials.fromStream(
                        new FileInputStream(KEY_PATH))).build();
        translate = options.getService();
    }
    
    @Function
    public void backToLogin(){
        ui.setActive(PAGE_LOGIN);
    }
    
    @Function
    public void register() throws Exception{
        if (!(ui.getUsername().equals("") || ui.getPassword().equals(""))){
        DatastoreOptions options = DatastoreOptions.newBuilder()
                .setProjectId(PROJECT_ID)
                .setCredentials(GoogleCredentials.fromStream(
                        new FileInputStream(KEY_PATH))).build();
        datastore = options.getService();
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(USERENTITY_KIND)
                .setFilter(PropertyFilter.eq(USERENTITY_USERNAME, ui.getUsername())).build();
        QueryResults<Entity> result = datastore.run(query);
        if (!result.hasNext()){ //weak check but should be no user with this username/password combination
          KeyFactory keyFactory = datastore.newKeyFactory().setKind(USERENTITY_KIND);
          IncompleteKey key = keyFactory.newKey();
          FullEntity<IncompleteKey> incEntity = Entity.newBuilder(key)
                  .set(USERENTITY_USERNAME, ui.getUsername())
                  .set(USERENTITY_PASSWORD, ui.getPassword()).build();
          datastore.add(incEntity);
          loadTitles(datastore);
          loadLanguages();
          ui.setActive(PAGE_MAINPAGE);
        }
    }
    }
    
    @Function
    public static void select(Data model,String data){
        model.setSelected(data);
    }
    
    @Function
    public static void selectLanguage(Data model, String data){
        System.out.println("Selected language is now: " + data);
        model.setSelectedLanguage(data);
    }
    
    public void translateStuff() throws Exception{
     //   File folder = new File("src\\main\\java\\com\\me\\cloudProject\\cloudProject\\TestProject-ddb9dc1083b8.json");
     if (ui.getSelectedTitle() != "" && ui.getTextcontent() != ""){
         if (translate == null){
             authenticateTranslate();
         }
        String code = getLanguageCode(ui.getSelectedLanguage());
        Translation translationTitle = translate.translate(ui.getSelectedTitle(), TranslateOption.targetLanguage(code));
        Translation translationContent = translate.translate(ui.getTextcontent(),TranslateOption.targetLanguage(code));
        ui.setTranslatedTitle(translationTitle.getTranslatedText());
        ui.setTranslatedContent(translationContent.getTranslatedText());
     }
                
    }
    
    private String getLanguageCode(String s){
        if (s!=null && s != "" && s.contains("(")){
            String code = s.split(Pattern.quote("("))[0].trim();
            return code;
        }
        return "en";
    }
    
    @Function
    public void printSelected() throws Exception{
        System.out.println("Selected: "+ui.getSelectedTitle());
        System.out.println("Selected language: "+ui.getSelectedLanguage());
    }
    
    @Function
    public void translateSelected() throws Exception{
        translateStuff();
    }
    
    @Function
    public void getSelected() throws Exception{
        System.out.println("Get selected runs and selceted value is: " + ui.getSelectedTitle());
        if (ui.getSelectedTitle()!="" && ui.getSelectedTitle() != null){
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(TEXTENTITY_KIND)
                .setFilter(PropertyFilter.eq(TEXTENTITY_TITLE, ui.getSelectedTitle()))
                .build();
        QueryResults<Entity> result = datastore.run(query);
        if (result.hasNext()){
            Entity e = result.next();
            String entityTitle = (String)e.getValue(TEXTENTITY_TITLE).get();
            String entityContent = (String)e.getValue(TEXTENTITY_CONTENT).get();
            ui.setSelectedTitle(entityTitle);
            ui.setTextcontent(entityContent);
        }
        }
        
    }
    
    private void loadTitles(Datastore datstore){
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind(TEXTENTITY_KIND).build();
        QueryResults<Entity> result = datstore.run(query);
        while(result.hasNext()){
            Entity e = result.next();
            ui.getTitles().add((String)e.getValue(TEXTENTITY_TITLE).get());
        }
    }
    
    private static Data ui;
    /**
     * Called when the page is ready.
     */
    static void onPageLoad(PlatformServices services) throws Exception {
        ui = new Data("","","","","","","","","");
        ui.setActive("login");
        ui.initServices(services);
        ui.applyBindings();
    }
}
