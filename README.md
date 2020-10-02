# OKULUS (anciennement Audit)

Elle est mise à la disposition des contrôleurs. Je l'ai conçue et developpée en Java, en utilisant Android Studio. La version 1.9 est actuellement disponible sur Google Play. Voici le déroulement typique de son utilisation :

**Première étape : Identification**  
    S'il ne s'est pas encore identifié, le contrôleur est dirigé vers la page d'identification, où il doit renseigner son adresse mail et son mot de passe (qui lui sont communiqués et qu'il a le droit de changer).
    Une fois identifié, il est dirigé vers la page de signature, où il enregistre la signature qui sera déposée sur tous les contrôles en son nom. Puis il est enfin dirigé vers la page d'accueil.  
    Implicitement, deux fichiers CSV sont téléchargés (du cloud Firebase). Le premier contient la liste des points à évaluer, le deuxième, la liste des marchés et de leurs intervenants respectifs, qui seront proposés dans la deuxième étape. En effet, ces listes sont évolutives et l'on veut toujours en avoir une version à jour. Cette vérification de synchronisation est effectuée à chaque lancement de l'application en présence de connexion internet. La date et l'heure de la dernière vérification de synchronisation de ces fichiers sont alors visibles sur la page d'accueil.
   
    
**Deuxième étape : Rédaction de l'entête**  
    La rédaction d'un nouvel "Audit" (contrôle) commence par la rédaction de l'entête : choisir le marché, l'intervenant et l'activité, et renseigner le nom du représentant de l'entreprise (technicien).
    
    
**Troisième étape : Remplissage du formulaire**  
    Le contrôleur procède ensuite à l'évaluation des points un par un. Puis, à la fin de chaque catégorie, il peut ajouter un commentaire et des photos (jusqu'à 30 photos). 
    
**Quatrième étape : Signature du technicien**  
    Une fois la rédaction terminée, on trouve un aperçu du fichier PDF généré. Le technicien pourra ainsi le parcourir avant d'apposer sa signature. Celle-ci sera déposée sur le PDF et est immédiatement supprimée de la mémoire du téléphone. De cette manière, toute autre modification du PDF va nécessiter une nouvelle signature.
    Plus implicitement, un fichier CSV est aussi généré contenant ces mêmes informations, pour pouvoir ensuite alimenter la base de données avec la version des données qui se prête mieux aux études statistiques, entre autres.
    
**Cinquième étape : Envoi du contrôle**  
    Dès que le contrôleur a une connexion internet, il doit aller sur la page "Mes Audits"  où il trouve la liste des contrôles qu'il a précedemment rédigés, triés du plus récent au plus ancien. Il peut ensuite cliquer sur le bouton "Envoyer" pour transmettre le fichier PDF et le fichier CSV concernant le contrôle sélectionné.
